package com.abujabal.nadim;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private SeekBar sbSpeed;
    private Switch swAuto;
    private EditText etName, etWords;
    private Spinner spKeyboard;
    private final String[] keyboards = {"الكيبورد العربي", "كيبورد جوجل", "كيبورد النظام"};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("nadim", Context.MODE_PRIVATE);

        sbSpeed = findViewById(R.id.sbSpeed);
        swAuto = findViewById(R.id.swAuto);
        etName = findViewById(R.id.etName);
        etWords = findViewById(R.id.etWords);
        spKeyboard = findViewById(R.id.spKeyboard);

        ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, keyboards);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spKeyboard.setAdapter(ad);

        sbSpeed.setProgress(prefs.getInt("speed", 200) - 1);
        swAuto.setChecked(prefs.getBoolean("auto", false));
        etName.setText(prefs.getString("name", ""));
        etWords.setText(prefs.getString("words", ""));
        spKeyboard.setSelection(prefs.getInt("keyboard", 0));

        findViewById(R.id.btnAccess).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        findViewById(R.id.btnOverlay).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            }
        });

        findViewById(R.id.btnImport).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("text/*");
                startActivityForResult(i, 77);
            }
        });

        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                prefs.edit()
                        .putInt("speed", sbSpeed.getProgress() + 1)
                        .putBoolean("auto", swAuto.isChecked())
                        .putString("name", etName.getText().toString().trim())
                        .putString("words", etWords.getText().toString())
                        .putInt("keyboard", spKeyboard.getSelectedItemPosition())
                        .apply();
                Toast.makeText(MainActivity.this, "تم الحفظ ✅", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 77 && res == RESULT_OK && data != null && data.getData() != null) {
            try {
                java.io.InputStream in = getContentResolver().openInputStream(data.getData());
                java.io.File out = new java.io.File(getFilesDir(), "words.txt");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
                fos.close();
                in.close();
                Toast.makeText(this, "تم استيراد الكلمات ✅", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "فشل: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
    }
