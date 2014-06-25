package tinytwit.ui;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import tinytwit.activities.R;

/**
 * Class to show a personalised toast.
 *
 * Created by Victor on 01/06/2014.
 */
public class SimpleToast {
    private static final String TAG = "SimpleToast";

    private Context context;

    /**
     * Constructor for the class
     *
     * @param context
     *            activity that will apply the toast
     */
    public SimpleToast(Context context) {
        this.context = context;
    }

    /**
     * Shows a message with the corresponding String obtained by the id
     *
     * @param text
     *            id of the string
     */
    public void toast(int text) {
        toast(context.getResources().getString(text));
    }

    /**
     * Shows one toast with the message of the parameter
     *
     * @param text
     *            message to show
     */
    public void toast(String text) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.toast_layout,
                (ViewGroup) ((Activity) context)
                        .findViewById(R.id.toast_layout_root));

        TextView textView = (TextView) layout.findViewById(R.id.text);
        textView.setText(text);
        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}
