package com.partyfm.radio.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;

class ParserM3U {

    LinkedList<String> getRawUrl(String url) {
        LinkedList<String> linkedList = new LinkedList<>();
        try {
            return getRawUrl(getConnection(url));
        } catch (IOException ignored) {

        }
        linkedList.add(url);
        return linkedList;
    }

    LinkedList<String> getRawUrl(URLConnection conn) {

        final BufferedReader bufferedReader;
        String s;
        LinkedList<String> murls = new LinkedList<>();
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
                        murls.add(s);
                    }
                } catch (IOException ignored) {

                }
            }
        } catch (IOException ignored) {

        }
        murls.add(conn.getURL().toString());
        return murls;
    }

    private String parseLine(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.contains("http")) {
            return trimmed.substring(trimmed.indexOf("http"));
        }
        return "";
    }

    private URLConnection getConnection(String url) throws IOException {
        return new URL(url).openConnection();
    }

}
