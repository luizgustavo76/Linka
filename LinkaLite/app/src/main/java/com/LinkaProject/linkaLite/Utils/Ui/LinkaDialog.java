package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class LinkaDialog {

    public static Dialog showLoading(Activity activity, String message) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_loading_ig, null);
        TextView txtMsg = (TextView) view.findViewById(R.id.txtLoadingMessage);
        if (txtMsg != null && message != null) {
            txtMsg.setText(message);
        }

        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }

    public static void showAlert(Activity activity, String title, String message) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_alert_ig, null);
        TextView txtTitle = (TextView) view.findViewById(R.id.txtDialogTitle);
        TextView txtMessage = (TextView) view.findViewById(R.id.txtDialogMessage);
        Button btnOk = (Button) view.findViewById(R.id.btnDialogOk);

        if (title == null || title.trim().length() == 0) {
            txtTitle.setVisibility(View.GONE);
        } else {
            txtTitle.setText(title);
        }
        txtMessage.setText(message);

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.setContentView(view);
        dialog.setCancelable(true);
        dialog.show();
    }
}