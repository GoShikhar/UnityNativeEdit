using System;
using UnityEngine;
using UnityEngine.UI;

public class Demo : MonoBehaviour
{
    private Text txt;

    private void Start()
    {
        txt = GetComponent<Text>();
    }

    public void OnEditValueChanged(string str)
    {
        txt.text = $"val changed: {str}";
    }

    public void OnEditEnded(string str)
    {
        txt.text = $"edit ended: {str}";
    }

    public void OnReturnPressed(NativeEditBox editBox)
    {
        //hide keyboard
        editBox.SetFocus(false);
        txt.text = "return pressed";
    }

    public void OnBeginEditing(NativeEditBox editBox)
    {
        txt.text = "begin editing";
    }
}