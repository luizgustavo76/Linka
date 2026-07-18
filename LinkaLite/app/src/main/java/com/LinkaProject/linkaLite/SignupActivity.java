package com.LinkaProject.linkaLite;
import com.LinkaProject.linkaLite.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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