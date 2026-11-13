package com.abujabal.nadim;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class NadimService extends AccessibilityService {

    private SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private long lastVolUp = 0;
    private boolean running = false;
    private final HashSet<String> used = new HashSet<>();

    private final String[] defaultWords = {
            "يا ابن الكحبة", "يا قحبة", "يا جبان", "يا فاشل", "يا حقير",
            "يا نذل", "يا خاين", "يا كلب", "يا حيوان", "يا قرد",
            "امك", "اختك", "اهلك", "شرفك", "وجهك"
    };

    @Override
    public void onServiceConnected() {
        prefs = getSharedPreferences("nadim", MODE_PRIVATE);
        String saved = prefs.getString("used", "");
        for (String s : saved.split("\n")) {
            if (!s.trim().isEmpty()) used.add(s.trim());
        }
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                long now = System.currentTimeMillis();
                if (now - lastVolUp < 700) {
                    lastVolUp = 0;
                    running = !running;
                } else {
                    lastVolUp = now;
                }
            }
            return true;
        }
        return super.onKeyEvent(event);
    }

    private ArrayList<String> words() {
        ArrayList<String> list = new ArrayList<>();
        try {
            File f = new File(getFilesDir(), "words.txt");
            if (f.exists()) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(new FileInputStream(f), "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty()) list.add(line.trim());
                }
                br.close();
            }
        } catch (Exception ignored) {}
        String w = prefs.getString("words", "");
        for (String s : w.split("\n")) {
            if (!s.trim().isEmpty()) list.add(s.trim());
        }
        if (list.isEmpty()) {
            for (String d : defaultWords) list.add(d);
        }
        return list;
    }

    private String sentence() {
        ArrayList<String> l = words();
        for (int attempt = 0; attempt < 40; attempt++) {
            int n = 2 + random.nextInt(3);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                if (i > 0) sb.append(" ");
                sb.append(l.get(random.nextInt(l.size())));
            }
            String out = injectName(sb.toString());
            if (!used.contains(out)) {
                used.add(out);
                prefs.edit().putString("used", TextUtils.join("\n", used)).apply();
                if (used.size() > 3000) used.clear();
                return out;
            }
        }
        return injectName(l.get(0));
    }

    private String injectName(String s) {
        String name = prefs.getString("name", "").trim();
        if (name.isEmpty()) return s;
        String[] parts = s.split(" ");
        int pos = random.nextInt(parts.length + 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i == pos) sb.append("يا ").append(name).append(" ");
            sb.append(parts[i]).append(" ");
        }
        if (pos == parts.length) sb.append("يا ").append(name);
        return sb.toString().trim();
    }

    private int delay() {
        int speed = prefs.getInt("speed", 200);
        if (speed < 1) speed = 1;
        if (speed > 500) speed = 500;
        return 501 - speed;
    }

    private AccessibilityNodeInfo focused() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        return root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
    }

    private void appendChar(final String ch) {
        AccessibilityNodeInfo node = focused();
        if (node == null) return;
        CharSequence cur = node.getText();
        String old = cur == null ? "" : cur.toString();
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, old + ch);
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }

    private void typeSentence() {
        if (!running) return;
        final String s = sentence() + " ";
        final int[] idx = {0};
        final Runnable[] r = new Runnable[1];
        r[0] = new Runnable() {
            public void run() {
                if (!running) return;
                if (idx[0] >= s.length()) {
                    handler.postDelayed(new Runnable() {
                        public void run() { typeSentence(); }
                    }, delay());
                    return;
                }
                appendChar(String.valueOf(s.charAt(idx[0])));
                idx[0]++;
                handler.postDelayed(r[0], delay());
            }
        };
        handler.post(r[0]);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {
        running = false;
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
    }
}
