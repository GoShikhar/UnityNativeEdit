package com.bkmin.android;

/**
 * Created by kyungminbang on 5/5/15.
 */

import org.json.JSONException;
import org.json.JSONObject;

import com.unity3d.player.UnityPlayer;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

/// UnityEditBox Plugin
/// Written by bkmin 2015/5 (kmin.bang@gmail.com)

public class EditBox {
    private EditText edit;
    private RelativeLayout	layout;
    private int tag;
    private String unityName;
    public boolean isActive = false;

    private static SparseArray<EditBox> mapEditBox = null;
    private static String MSG_CREATE = "CreateEdit";
    private static String MSG_REMOVE = "RemoveEdit";
    private static String MSG_SET_TEXT = "SetText";
    private static String MSG_GET_TEXT = "GetText";
    private static String MSG_SET_RECT = "SetRect";
    private static String MSG_SET_FOCUS = "SetFocus";
    private static String MSG_SET_VISIBLE = "SetVisible";
    private static String MSG_TEXT_CHANGE = "TextChange";
    private static String MSG_TEXT_END_EDIT = "TextEndEdit";
    private static String MSG_ANDROID_KEY_DOWN = "AndroidKeyDown";
    private static String MSG_RETURN_PRESSED = "ReturnPressed";

    public static JSONObject makeJsonRet(boolean isError, String strError)
    {
        JSONObject json = new JSONObject();
        try
        {
            json.put("bError", isError);
            json.put("strError", strError);
        }
        catch(JSONException e) {}
        return json;
    }

    public static JSONObject processRecvJsonMsg(int nSenderId, final String strJson)
    {
        JSONObject jsonRet = null;

        if (mapEditBox == null) mapEditBox = new SparseArray<EditBox>();

        try
        {
            JSONObject jsonMsg = new JSONObject(strJson);
            String msg = jsonMsg.getString("msg");

            if (msg.equals(MSG_CREATE))
            {
                EditBox nb = new EditBox(NativeEditPlugin.mainLayout);
                nb.Create(nSenderId, jsonMsg);
                mapEditBox.append(nSenderId, nb);
                jsonRet = makeJsonRet(false, "");
            }
            else
            {
                EditBox eb =  mapEditBox.get(nSenderId);
                if (eb != null) {
                    jsonRet = eb.processJsonMsg(jsonMsg);
                }
                else
                {
                    Log.e(NativeEditPlugin.LOG_TAG, "EditBox not found, id : " + nSenderId);
                }
            }


        } catch (JSONException e)
        {
        }
        return jsonRet;
    }

    public EditBox(RelativeLayout mainLayout)
    {
        layout = mainLayout;
        edit = null;
    }

    public void showKeyboard(boolean isShow)
    {
        InputMethodManager imm = (InputMethodManager) NativeEditPlugin.unityActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (isShow)
        {
            imm.showSoftInput(edit, InputMethodManager.SHOW_FORCED);
        }
        else
        {
            View rootView = NativeEditPlugin.unityActivity.getWindow().getDecorView();
            rootView.clearFocus();
            imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
        }
    }

    public JSONObject processJsonMsg(JSONObject jsonMsg)
    {
        JSONObject jsonRet = makeJsonRet(false, "");
        try
        {
            String msg = jsonMsg.getString("msg");

            if (msg.equals(MSG_REMOVE))
            {
                this.Remove();
            }
            else if (msg.equals(MSG_SET_TEXT))
            {
                String text = jsonMsg.getString("text");
                this.SetText(text);
            }
            else if (msg.equals(MSG_GET_TEXT))
            {
                String text = this.GetText();
                jsonRet.put("text", text);
            }
            else if (msg.equals(MSG_SET_RECT))
            {
                this.SetRect(jsonMsg);
            }
            else if (msg.equals(MSG_SET_FOCUS))
            {
                boolean isFocus = jsonMsg.getBoolean("isFocus");
                this.SetFocus(isFocus);
            }
            else if (msg.equals(MSG_SET_VISIBLE))
            {
                boolean isVisible = jsonMsg.getBoolean("isVisible");
                this.SetVisible(isVisible);
            }
            else if (msg.equals(MSG_ANDROID_KEY_DOWN))
            {
                String strKey = jsonMsg.getString("key");
                this.OnForceAndroidKeyDown(strKey);
            }

        } catch (JSONException e)
        {
        }
        return jsonRet;
    }

    public void SendJsonToUnity(JSONObject jsonToUnity)
    {
        try
        {
            jsonToUnity.put("senderId", this.tag);
        }
        catch(JSONException e) {}
        NativeEditPlugin.SendUnityMessage(jsonToUnity);
    }

    public void Create(int _tag, JSONObject jsonObj)
    {
        this.tag = _tag;

        try {
            String placeHolder = jsonObj.getString("placeHolder");

            String font = jsonObj.getString("font");
            double fontSize = jsonObj.getDouble("fontSize");

            double x = jsonObj.getDouble("x") * (double) layout.getWidth();
            double y = jsonObj.getDouble("y") * (double) layout.getHeight();
            double width = jsonObj.getDouble("width") * (double) layout.getWidth();
            double height = jsonObj.getDouble("height") * (double) layout.getHeight();

            int textColor_r = (int) (255.0f * jsonObj.getDouble("textColor_r"));
            int textColor_g = (int) (255.0f * jsonObj.getDouble("textColor_g"));
            int textColor_b = (int) (255.0f * jsonObj.getDouble("textColor_b"));
            int textColor_a = (int) (255.0f * jsonObj.getDouble("textColor_a"));
            int backColor_r = (int) (255.0f * jsonObj.getDouble("backColor_r"));
            int backColor_g = (int) (255.0f * jsonObj.getDouble("backColor_g"));
            int backColor_b = (int) (255.0f * jsonObj.getDouble("backColor_b"));
            int backColor_a = (int) (255.0f * jsonObj.getDouble("backColor_a"));

            String contentType = jsonObj.getString("contentType");
            String inputType = jsonObj.optString("inputType");
            String keyboardType = jsonObj.optString("keyboardType");
            String characterValidation = jsonObj.optString("characterValidation");
            String returnKeyType = jsonObj.getString("return_key_type");

            String alignment = jsonObj.getString("align");
            boolean withDoneButton = jsonObj.getBoolean("withDoneButton");
            boolean multiline = jsonObj.getBoolean("multiline");

            edit = new EditText(NativeEditPlugin.unityActivity.getApplicationContext());

            // It's important to set this first as it resets some things, for example character hiding if content type is password.
            edit.setSingleLine(!multiline);

            edit.setId(0);
            edit.setText("");
            edit.setHint(placeHolder);

            Rect rect = new Rect((int) x, (int) y, (int) (x + width), (int) (y + height));
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(rect.width(), rect.height());
            lp.setMargins(rect.left, rect.top, 0, 0);
            edit.setLayoutParams(lp);
            edit.setPadding(0, 0, 0, 0);

            int editInputType = 0;
            switch (contentType) {
                case "Standard" : editInputType |= InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_VARIATION_NORMAL; break; // This is default behaviour
                case "Autocorrected" : editInputType |= InputType.TYPE_CLASS_TEXT  | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT; break;
                case "IntegerNumber" : editInputType |= InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL; break;
                case "DecimalNumber" : editInputType |= InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL; break;
                case "Alphanumeric" : editInputType |= InputType.TYPE_CLASS_TEXT  | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_VARIATION_NORMAL; break; // This is default behaviour
                case "Name" : editInputType |= InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME; break;
                case "EmailAddress" : editInputType |= InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS; break;
                case "Password" : editInputType |= InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD; break;
                case "Pin" : editInputType |= InputType.TYPE_CLASS_PHONE; break;

                case "Custom" : // We need more details
                    switch (keyboardType) {
                        case "ASCIICapable" : editInputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS; break;
                        case "NumbersAndPunctuation" : editInputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL |InputType.TYPE_NUMBER_FLAG_SIGNED; break;
                        case "URL" : editInputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_URI; break;
                        case "NumberPad" : editInputType = InputType.TYPE_CLASS_NUMBER;  break;
                        case "PhonePad" : editInputType = InputType.TYPE_CLASS_PHONE;  break;
                        case "NamePhonePad" : editInputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME; break;
                        case "EmailAddress" : editInputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS; break;
                        default :  editInputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL;
                    }

                    if (multiline) editInputType  |=  InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;

                    switch (inputType) {
                        case "AutoCorrect" : editInputType |=  InputType.TYPE_TEXT_FLAG_AUTO_CORRECT; break;
                        case "Password" : editInputType |=  InputType.TYPE_NUMBER_VARIATION_PASSWORD | InputType.TYPE_TEXT_VARIATION_PASSWORD ; break;
                    }
                    break;

                default : editInputType |= InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL; break; // No action

            }

            edit.setInputType(editInputType);

            int gravity = 0;
            if (alignment.equals("UpperLeft"))
            {
                gravity = Gravity.TOP | Gravity.LEFT;
            }
            else if (alignment.equals("UpperCenter"))
            {
                gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            }
            else if (alignment.equals("UpperRight"))
            {
                gravity = Gravity.TOP | Gravity.RIGHT;
            }
            else if (alignment.equals("MiddleLeft"))
            {
                gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
            }
            else if (alignment.equals("MiddleCenter"))
            {
                gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
            }
            else if (alignment.equals("MiddleRight"))
            {
                gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
            }
            else if (alignment.equals("LowerLeft"))
            {
                gravity = Gravity.BOTTOM | Gravity.LEFT;
            }
            else if (alignment.equals("LowerCenter"))
            {
                gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            }
            else if (alignment.equals("LowerRight"))
            {
                gravity = Gravity.BOTTOM | Gravity.RIGHT;
            }

            int imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI;
            if (returnKeyType.equals("Next")) {
                imeOptions |= EditorInfo.IME_ACTION_NEXT;
            }
            else if (returnKeyType.equals("Done")) {
                imeOptions |= EditorInfo.IME_ACTION_DONE;
            }
            edit.setImeOptions(imeOptions);

            edit.setGravity(gravity);


            edit.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) fontSize);
            edit.setTextColor(Color.argb(textColor_a, textColor_r, textColor_g, textColor_b));
            edit.setBackgroundColor(Color.argb(backColor_a, backColor_r, backColor_g, backColor_b));

            Typeface tf = Typeface.create(font, Typeface.NORMAL);
            edit.setTypeface(tf);

            final EditBox eb = this;

            edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        // your action here
                        JSONObject jsonToUnity = new JSONObject();
                        try
                        {
                            jsonToUnity.put("msg", MSG_TEXT_END_EDIT);
                            jsonToUnity.put("text", eb.GetText());
                        }
                        catch(JSONException e) {}
                        eb.SendJsonToUnity(jsonToUnity);
                        eb.showKeyboard(false);
                    }
                }
            });

            edit.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s)
                {
                    JSONObject jsonToUnity = new JSONObject();
                    try
                    {
                        jsonToUnity.put("msg", MSG_TEXT_CHANGE);
                        jsonToUnity.put("text", s.toString());
                    }
                    catch(JSONException e) {}
                    eb.SendJsonToUnity(jsonToUnity);
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {
                    // TODO Auto-generated method stub

                }
            });

            edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if ((actionId == EditorInfo.IME_ACTION_DONE) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                        JSONObject jsonToUnity = new JSONObject();
                        try
                        {
                            jsonToUnity.put("msg", MSG_RETURN_PRESSED);
                        }
                        catch(JSONException e) {}

                        eb.SendJsonToUnity(jsonToUnity);
                        return true;
                    }
                    return false;
                }
            });

            layout.setFocusableInTouchMode(true);
            layout.setClickable(true);
            layout.addView(edit);

        } catch (JSONException e)
        {
            Log.i(NativeEditPlugin.LOG_TAG, String.format("Create editbox error %s", e.getMessage()));
            return;
        }
    }

    public void Remove()
    {
        if (edit != null) {
            layout.removeView(edit);
        }
        edit = null;
    }

    public void SetText(String newText)
    {
        if (edit != null) {
            edit.setText(newText);
        }
    }
    public String GetText()
    {
        return edit.getText().toString();
    }

    public boolean isFocused()
    {
        return edit.isFocused();
    }

    public void SetFocus(boolean isFocus)
    {
        if (isFocus)
        {
            edit.requestFocus();
        }
        else
        {
            edit.clearFocus();

        }
        this.showKeyboard(isFocus);
    }

    public void SetRect(JSONObject jsonRect)
    {
        try
        {
            double x = jsonRect.getDouble("x") * (double) layout.getWidth();
            double y = jsonRect.getDouble("y") * (double) layout.getHeight();
            double width = jsonRect.getDouble("width") * (double) layout.getWidth();
            double height = jsonRect.getDouble("height") * (double) layout.getHeight();

            Rect rect = new Rect((int) x, (int) y, (int) (x + width), (int) (y + height));
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(rect.width(), rect.height());
            lp.setMargins(rect.left, rect.top, 0, 0);
            edit.setLayoutParams(lp);

        } catch (JSONException e)
        {
            return;
        }
    }

    public void SetVisible(boolean bVisible)
    {
        edit.setEnabled(bVisible);
        edit.setVisibility(bVisible ? View.VISIBLE : View.INVISIBLE);
    }

    public void OnForceAndroidKeyDown(String strKey)
    {
        if (!this.isFocused()) return;

        // Need to force fire key event of backspace and enter because Unity eats them and never return back to plugin.
        int keyCode = -1;
        if (strKey.equalsIgnoreCase("backspace"))
        {
            keyCode = KeyEvent.KEYCODE_DEL;
        }
        else if (strKey.equalsIgnoreCase("enter"))
        {
            keyCode = KeyEvent.KEYCODE_ENTER;
        }
        if (keyCode > 0)
        {
            KeyEvent ke = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
            Log.i(NativeEditPlugin.LOG_TAG, String.format("Force fire KEY EVENT %d", keyCode));
            edit.onKeyDown(keyCode, ke);
        }
    }
}
