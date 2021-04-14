package com.partyfm.radio.services;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;

class ParserASX {

    LinkedList<String> getRawUrl(String url) {
        LinkedList<String> linkedList = null;
        try {
            return getRawUrl(getConnection(url));
        } catch (IOException ignored) {

        }
        return linkedList;
    }

    private LinkedList<String> getRawUrl(URLConnection conn) {

        final BufferedReader bufferedReader;
        String s;
        LinkedList<String> linkedList;
        linkedList = new LinkedList<>();
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            while (true) {
                try {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    s = parseLine(line);
                    if (s != null && !s.equals("")) {
                        linkedList.add(s);
                    }
                } catch (IOException ignored) {

                }
            }
        } catch (IOException ignored) {

        }

        return linkedList;
    }

    private String parseLine(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.startsWith("<ref href=\"")) {
            trimmed = trimmed.replace("<ref href=\"", "");
            trimmed = trimmed.replace("/>", "").trim();
            if (trimmed.endsWith("\"")) {
                trimmed = trimmed.replace("\"", "");
                Log.v("INFO", "ASX: " + trimmed);
                return trimmed;
            }
        }
        return "";
    }

    private URLConnection getConnection(String url) throws IOException {
        return new URL(url).openConnection();
    }

}
