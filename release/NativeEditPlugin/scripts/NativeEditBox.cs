/*
 * Copyright (c) 2015 Kyungmin Bang
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


/*
 *  NativeEditBox script should be attached to Unity UI InputField object 
 * 
 *  Limitation
 * 
 * 1. Screen auto rotation is not supported.
 */


using System;
using System.Collections;
using UnityEngine;
using UnityEngine.Events;
using UnityEngine.UI;

[RequireComponent(typeof(InputField))]
public class NativeEditBox : PluginMsgReceiver
{
    private struct EditBoxConfig
    {
        public bool multiline;
        public Color textColor;
        public Color backColor;
        public string contentType;
        public string font;
        public float fontSize;
        public string align;
        public string placeHolder;
        public int characterLimit;
        public Color placeHolderColor;
    }

    public enum ReturnKeyType
    {
        Default,
        Next,
        Done,
        Send,
        Go
    }

    public float androidUpdateDeltaTime = 0.15f;
    public bool iOSWithDoneButton = true;
    public bool useInputFieldFont;
    public bool clearFocusOnReturnPressed = true;
    public ReturnKeyType singleLineReturnKeyType;

    public event Action returnPressed;
    public UnityEvent onReturnPressed; // only invoke on iOS & Android
    public UnityEvent OnBeginEdit; // only invoke on iOS & Android

    private bool _hasNativeEditCreated;

    private Text _textComponent;
    private bool _focusOnCreate;
    private bool _visibleOnCreate = true;
    private float _fakeTimer = 0f;

    private const string MSG_CREATE = "CreateEdit";
    private const string MSG_REMOVE = "RemoveEdit";
    private const string MSG_SET_TEXT = "SetText";
    private const string MSG_SET_RECT = "SetRect";
    private const string MSG_SET_TEXTSIZE = "SetTextSize";
    private const string MSG_SET_FOCUS = "SetFocus";
    private const string MSG_SET_VISIBLE = "SetVisible";
    private const string MSG_TEXT_CHANGE = "TextChange";
    private const string MSG_TEXT_BEGIN_EDIT = "TextBeginEdit";
    private const string MSG_TEXT_END_EDIT = "TextEndEdit";

    // to fix bug Some keys 'back' & 'enter' are eaten by unity and never arrive at plugin
    private const string MSG_ANDROID_KEY_DOWN = "AndroidKeyDown";
    private const string MSG_RETURN_PRESSED = "ReturnPressed";
    private const string MSG_GET_TEXT = "GetText";

    public InputField inputField { get; private set; }

    public bool visible { get; private set; }

    public string text
    {
        get { return inputField.text; }
        set
        {
            inputField.text = value;
            if(_hasNativeEditCreated)
                SetTextNative(value);
        }
    }

    public static Rect GetScreenRectFromRectTransform(RectTransform rectTransform)
    {
        var corners = new Vector3[4];

        rectTransform.GetWorldCorners(corners);

        var xMin = float.PositiveInfinity;
        var xMax = float.NegativeInfinity;
        var yMin = float.PositiveInfinity;
        var yMax = float.NegativeInfinity;

        for(var i = 0; i < 4; i++)
        {
            // For Canvas mode Screen Space - Overlay there is no Camera;
            // the best solution I've found is to use RectTransformUtility.WorldToScreenPoint with a null camera.
            // When screen match mode of Canvas Scaler is set to Match Width Or Height, be sure that it matches height completely or there will be mistakes.
            Vector3 screenCoord = RectTransformUtility.WorldToScreenPoint(null, corners[i]);

            if(screenCoord.x < xMin)
                xMin = screenCoord.x;
            if(screenCoord.x > xMax)
                xMax = screenCoord.x;
            if(screenCoord.y < yMin)
                yMin = screenCoord.y;
            if(screenCoord.y > yMax)
                yMax = screenCoord.y;
        }

        var result = new Rect(xMin, Screen.height - yMax, xMax - xMin, yMax - yMin);
        return result;
    }

    private EditBoxConfig mConfig;

    private void Awake()
    {
        inputField = GetComponent<InputField>();
        if(inputField == null)
        {
            Debug.LogErrorFormat("No InputField found {0} NativeEditBox Error", name);
            throw new MissingComponentException();
        }

        _textComponent = inputField.textComponent;
    }

    // Use this for initialization
    protected override void Start()
    {
        base.Start();

        // Wait until the end of frame before initializing to ensure that Unity UI layout has been built. We used to
        // initialize at Start, but that resulted in an invalid RectTransform position and size on the InputField if it
        // was instantiated at runtime instead of being built in to the scene.
        StartCoroutine(InitializeOnNextFrame());
    }

    private void OnEnable()
    {
        if(_hasNativeEditCreated)
            SetVisible(true);
    }

    private void OnDisable()
    {
        if(_hasNativeEditCreated)
            SetVisible(false);
    }

    protected override void OnDestroy()
    {
        if(!_hasNativeEditCreated)
            return;

        RemoveNative();
        base.OnDestroy();
    }

    private void OnApplicationPause(bool pause)
    {
        if(!_hasNativeEditCreated)
            return;

        SetVisible(!pause);
    }

    private IEnumerator InitializeOnNextFrame()
    {
        yield return null;

        PrepareNativeEdit();
#if (UNITY_IPHONE || UNITY_ANDROID) && !UNITY_EDITOR
		CreateNativeEdit();
		SetTextNative(inputField.text);

		inputField.placeholder.gameObject.SetActive(false);
		_textComponent.enabled = false;
		inputField.enabled = false;
#endif
    }

    private void Update()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
		UpdateForceKeyeventForAndroid();

		//Plugin has to update rect continually otherwise we cannot see characters inputted just now 
		_fakeTimer += Time.deltaTime;
		if (_fakeTimer >= androidUpdateDeltaTime && inputField != null && _hasNativeEditCreated && visible)
		{
			SetRectNative(_textComponent.rectTransform);
			_fakeTimer = 0f;
		}
#endif
    }

    private void PrepareNativeEdit()
    {
        var placeHolder = inputField.placeholder.GetComponent<Text>();

        if(useInputFieldFont)
        {
            var font = _textComponent.font;
            mConfig.font = font.fontNames.Length > 0 ? font.fontNames[0] : "Arial";
        }

        mConfig.placeHolder = placeHolder.text;
        mConfig.placeHolderColor = placeHolder.color;
        mConfig.characterLimit = inputField.characterLimit;

        var rectScreen = GetScreenRectFromRectTransform(_textComponent.rectTransform);
        var fHeightRatio = rectScreen.height / _textComponent.rectTransform.rect.height;
        mConfig.fontSize = _textComponent.fontSize * fHeightRatio;

        mConfig.textColor = _textComponent.color;
        mConfig.align = _textComponent.alignment.ToString();
        mConfig.contentType = inputField.contentType.ToString();
        mConfig.backColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);
        mConfig.multiline = inputField.lineType != InputField.LineType.SingleLine;
    }

    public override void OnPluginMsgDirect(JsonObject jsonMsg)
    {
        PluginMsgHandler.GetInstanceForReceiver(this).StartCoroutine(PluginsMessageRoutine(jsonMsg));
    }

    private IEnumerator PluginsMessageRoutine(JsonObject jsonMsg)
    {
        // this is to avoid a deadlock for more info when trying to get data from two separate native plugins and handling them in Unity
        yield return null;

        var msg = jsonMsg.GetString("msg");
        if(msg.Equals(MSG_TEXT_BEGIN_EDIT))
        {
            OnBeginEdit?.Invoke();
        }
        else if(msg.Equals(MSG_TEXT_CHANGE) || msg.Equals(MSG_TEXT_END_EDIT))
        {
            inputField.text = jsonMsg.GetString("text");
        }
        else if(msg.Equals(MSG_RETURN_PRESSED))
        {
            returnPressed?.Invoke();
            onReturnPressed?.Invoke();
            if(clearFocusOnReturnPressed)
                SetFocus(false);
        }
    }

    private bool CheckErrorJsonRet(JsonObject jsonRet)
    {
        var bError = jsonRet.GetBool("bError");
        var strError = jsonRet.GetString("strError");
        if(bError) Debug.LogError($"NativeEditbox error {strError}");
        return bError;
    }

    private void CreateNativeEdit()
    {
        var rectScreen = GetScreenRectFromRectTransform(_textComponent.rectTransform);

        var jsonMsg = new JsonObject
        {
            ["msg"] = MSG_CREATE,
            ["x"] = rectScreen.x / Screen.width,
            ["y"] = rectScreen.y / Screen.height,
            ["width"] = rectScreen.width / Screen.width,
            ["height"] = rectScreen.height / Screen.height,
            ["characterLimit"] = mConfig.characterLimit,
            ["textColor_r"] = mConfig.textColor.r,
            ["textColor_g"] = mConfig.textColor.g,
            ["textColor_b"] = mConfig.textColor.b,
            ["textColor_a"] = mConfig.textColor.a,
            ["backColor_r"] = mConfig.backColor.r,
            ["backColor_g"] = mConfig.backColor.g,
            ["backColor_b"] = mConfig.backColor.b,
            ["backColor_a"] = mConfig.backColor.a,
            ["font"] = mConfig.font,
            ["fontSize"] = mConfig.fontSize,
            ["contentType"] = mConfig.contentType,
            ["align"] = mConfig.align,
            ["withDoneButton"] = iOSWithDoneButton,
            ["placeHolder"] = mConfig.placeHolder,
            ["placeHolderColor_r"] = mConfig.placeHolderColor.r,
            ["placeHolderColor_g"] = mConfig.placeHolderColor.g,
            ["placeHolderColor_b"] = mConfig.placeHolderColor.b,
            ["placeHolderColor_a"] = mConfig.placeHolderColor.a,
            ["multiline"] = mConfig.multiline
        };
        switch(singleLineReturnKeyType)
        {
            case ReturnKeyType.Next:
                jsonMsg["return_key_type"] = "Next";
                break;
            case ReturnKeyType.Done:
                jsonMsg["return_key_type"] = "Done";
                break;
            case ReturnKeyType.Send:
                jsonMsg["return_key_type"] = "Send";
                break;
            case ReturnKeyType.Go:
                jsonMsg["return_key_type"] = "Go";
                break;
            default:
                jsonMsg["return_key_type"] = "Default";
                break;
        }

        var jsonRet = SendPluginMsg(jsonMsg);
        _hasNativeEditCreated = !CheckErrorJsonRet(jsonRet);

        visible = _visibleOnCreate;
        if(!_visibleOnCreate)
            SetVisible(false);

        if(_focusOnCreate)
            SetFocus(true);
    }

    private void SetTextNative(string newText)
    {
        var jsonMsg = new JsonObject
        {
            ["msg"] = MSG_SET_TEXT,
            ["text"] = newText ?? string.Empty
        };
        SendPluginMsg(jsonMsg);
    }

    private void RemoveNative()
    {
        var jsonMsg = new JsonObject
        {
            ["msg"] = MSG_REMOVE
        };
        SendPluginMsg(jsonMsg);
    }

    public void SetRectNative(RectTransform rectTrans)
    {
        var rectScreen = GetScreenRectFromRectTransform(rectTrans);

        var jsonMsg = new JsonObject
        {
            ["msg"] = MSG_SET_RECT,
            ["x"] = rectScreen.x / Screen.width,
            ["y"] = rectScreen.y / Screen.height,
            ["width"] = rectScreen.width / Screen.width,
            ["height"] = rectScreen.height / Screen.height
        };
        SendPluginMsg(jsonMsg);

        var fontRectHeightRatio = rectScreen.height / _textComponent.rectTransform.rect.height;
        var fontSize = _textComponent.fontSize * fontRectHeightRatio;
        if(Math.Abs(mConfig.fontSize - fontSize) > 0.1f)
        {
            var sizeMsg = new JsonObject
            {
                ["msg"] = MSG_SET_TEXTSIZE,
                ["fontSize"] = fontSize
            };
            SendPluginMsg(sizeMsg);
            mConfig.fontSize = fontSize;
        }
    }

    public void SetFocus(bool bFocus)
    {
#if (UNITY_IOS || UNITY_ANDROID) && !UNITY_EDITOR
		if (!_hasNativeEditCreated)
		{
			_focusOnCreate = bFocus;
			return;
		}

		var jsonMsg = new JsonObject
        {
		    ["msg"] = MSG_SET_FOCUS,
		    ["isFocus"] = bFocus
        };
		SendPluginMsg(jsonMsg);
#else
        if(gameObject.activeInHierarchy)
        {
            if(bFocus)
                inputField.ActivateInputField();
            else
                inputField.DeactivateInputField();
        }
        else
        {
            _focusOnCreate = bFocus;
        }
#endif
    }

    public void SetVisible(bool bVisible)
    {
        if(!_hasNativeEditCreated)
        {
            _visibleOnCreate = bVisible;
            return;
        }

        var jsonMsg = new JsonObject
        {
            ["msg"] = MSG_SET_VISIBLE,
            ["isVisible"] = bVisible
        };
        SendPluginMsg(jsonMsg);

        visible = bVisible;
    }

#if UNITY_ANDROID && !UNITY_EDITOR
	private void ForceSendKeydown_Android(string key)
	{
		var jsonMsg = new JsonObject
        {
		    ["msg"] = MSG_ANDROID_KEY_DOWN,
		    ["key"] = key
        };
		SendPluginMsg(jsonMsg);
	}

	private void UpdateForceKeyeventForAndroid()
	{
		if (Input.anyKeyDown)
		{
			if (Input.GetKeyDown(KeyCode.Backspace))
			{
				ForceSendKeydown_Android("backspace");
			}
			else
			{
				foreach(char c in Input.inputString)
				{
					if (c == '\n')
					{
						ForceSendKeydown_Android("enter");
					}
					else
					{
						ForceSendKeydown_Android(Input.inputString);
					}
				}
			}
		}
	}
#endif
}