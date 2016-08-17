package com.google.blockly.android.ui.fieldview;

import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.NumberKeyListener;

public class SignedDecimalKeyListener extends NumberKeyListener
{
    private char[] mAccepted;
    private static SignedDecimalKeyListener sInstance;

    private static final char[] CHARACTERS = new char[] { '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' };

    @Override
    protected char[] getAcceptedChars()
    {
        return mAccepted;
    }

    private SignedDecimalKeyListener()
    {
        mAccepted = CHARACTERS;
    }

    public static SignedDecimalKeyListener getInstance()
    {
        if(sInstance != null)
            return sInstance;
        sInstance = new SignedDecimalKeyListener();
        return sInstance;
    }

    public int getInputType()
    {
        return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
    {
        CharSequence out = super.filter(source, start, end, dest, dstart, dend);

        if(out != null)
        {
            source = out;
            start = 0;
            end = out.length();
        }

        //Only allow a '-' to be the very first char
        //and don't allow '.' to be the first char
        if(dstart > 0 && source.equals("-") || dstart == 0 && source.equals("."))
        {
            SpannableStringBuilder stripped = null;

            stripped = new SpannableStringBuilder(source, start, end);
            stripped.delete(start, end);

            if(stripped != null)
                return stripped;
        }
        else if(source.equals("."))
        {
            for(int lo = dend-1; lo > 0; lo--)
            {
                char c = dest.charAt(lo);
                if(source.equals(String.valueOf(c)))
                {
                    SpannableStringBuilder stripped = null;

                    stripped = new SpannableStringBuilder(source, start, end);
                    stripped.delete(start, end);

                    if(stripped != null)
                        return stripped;
                }
            }
        }

        if(out != null)
            return out;
        else
            return null;
    }
}