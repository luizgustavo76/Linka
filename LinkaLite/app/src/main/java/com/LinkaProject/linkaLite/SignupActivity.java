package com.LinkaProject.linkaLite;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
public class SignupActivity extends Activity {
    private EditText edtUsername;
    private EditText edtPassword;
    private EditText edtRetypePassword;
    private EditText edtEmail;
    private TextView txtGoToSignin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        edtUsername = (EditText) findViewById(R.id.edtUsername);
        edtPassword = (EditText) findViewById(R.id.edtPassword); 
        edtRetypePassword = (EditText) findViewById(R.id.edtRetypePassword);
        edtEmail = (EditText) findViewById(R.id.edtEmail);
        txtGoToSignin = (TextView) findViewById(R.id.txtGoToSignin);
    }
}