package com.abujabal.nadim;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

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

    @Override
    public void onServiceConnected() {
        prefs = getSharedPreferences("nadim", MODE_PRIVATE);
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                long now = System.currentTimeMillis();
                if (now - lastVolUp < 700) {
                    lastVolUp = 0;
                    toggleTyping();
                } else {
                    lastVolUp = now;
                }
            }
            return true; // نمنع تغيير الصوت وظهور الشاشة
        }
        return super.onKeyEvent(event);
    }

    private void toggleTyping() {
        if (running) {
            running = false;
        } else {
            running = true;
            startTyping();
        }
    }

    private void startTyping() {
        handler.post(new Runnable() {
            public void run() {
                if (!running) return;
                typeOnce();
                int speed = prefs.getInt("speed", 300);
                handler.postDelayed(this, speed);
            }
        });
    }

    private ArrayList<String> words() {
        ArrayList<String> list = new ArrayList<>();
        String w = prefs.getString("words", "");
        for (String s : w.split("\n")) {
            if (!s.trim().isEmpty()) list.add(s.trim());
        }
        if (list.isEmpty()) list.add("كلمة");
        return list;
    }

    private String sentence() {
        ArrayList<String> l = words();
        String out;
        for (int attempt = 0; attempt < 40; attempt++) {
            StringBuilder sb = new StringBuilder();
            int n = 2 + random.nextInt(3);
            for (int i = 0; i < n; i++) {
                if (i > 0) sb.append(" ");
                sb.append(l.get(random.nextInt(l.size())));
            }
            out = sb.toString();
            if (!used.contains(out)) {
                used.add(out);
                if (used.size() > 2000) used.clear();
                return injectName(out);
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

    private void typeOnce() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused == null) return;
        String text = sentence();
        Bundle args = new Bundle();
        String old = focused.getText() == null ? "" : focused.getText().toString();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, old + text);
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
  }
