package linka.tlsdemo;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;

public final class DemoActivity extends Activity {
    private EditText endpoint;
    private EditText fingerprint;
    private TextView results;
    private Button single;
    private Button batch;

    public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        endpoint = new EditText(this);
        endpoint.setSingleLine(true);
        endpoint.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        endpoint.setText("https://10.0.2.2:8443/hello");
        form.addView(endpoint);
        fingerprint = new EditText(this);
        fingerprint.setSingleLine(true);
        fingerprint.setHint("Certificate SHA-256 fingerprint");
        form.addView(fingerprint);
        single = addButton(form, "Make 1 call", 1);
        batch = addButton(form, "Make 50 calls", 50);
        results = new TextView(this);
        form.addView(results);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        setContentView(scroll);
    }

    private Button addButton(LinearLayout form, String label, final int count) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                runCalls(count);
            }
        });
        form.addView(button);
        return button;
    }

    private void runCalls(final int count) {
        final String url = endpoint.getText().toString().trim();
        final String pin = fingerprint.getText().toString();
        results.setText("");
        single.setEnabled(false);
        batch.setEnabled(false);
        new Thread(new Runnable() {
            public void run() {
                long started = System.nanoTime();
                int successes = 0;
                try {
                    TlsHello client = new TlsHello(url, pin);
                    for (int i = 1; i <= count; i++) {
                        try {
                            String body = client.get();
                            successes++;
                            append(i + "/" + count + " " + body);
                        } catch (IOException e) {
                            append(i + "/" + count + " " + e);
                        }
                    }
                } catch (IOException e) {
                    append(e.toString());
                } finally {
                    append(successes + "/" + count + " successful in "
                            + ((System.nanoTime() - started) / 1000000L) + " ms");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            single.setEnabled(true);
                            batch.setEnabled(true);
                        }
                    });
                }
            }
        }, "hello-demo").start();
    }

    private void append(final String line) {
        runOnUiThread(new Runnable() {
            public void run() {
                results.append(line + "\n");
            }
        });
    }
}
