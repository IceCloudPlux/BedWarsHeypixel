/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.andrei1058.bedwars.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class LicenseChecker {

    // Encrypted URL fragments (XOR + base64 encoded)
    // Original URL: https://raw.githubusercontent.com/IceCloudPlux/IceCloudPlux-Website/main/_data/licenses.txt
    private static final String[] ENC_FRAG = {
        "VEhITE8=", "eWxsMSI=", "PWQtIz4=", "OSQzJCI=", "PSo7NzY=",
        "KzoxK3E=", "BQkLSS8=", "DgguAQI=", "ARAkGAE=", "A1QyGB4=",
        "we7t9+Y=", "2eX88aQ=", "x/Xy4/k=", "4/K4+vY=", "9/Cxwfo=",
        "xNHEisk=", "xc/Jwt8=", "1sCdx8s=", "zg=="
    };

    private static final String USER_AGENT = "BedWars1058-Lic/2.0";
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;

    public static boolean verify(String licenseKey) {
        if (licenseKey == null || licenseKey.trim().isEmpty()) return false;

        try {
            String url = decryptUrl();
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "text/plain");

            Set<String> validKeys = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        validKeys.add(trimmed);
                    }
                }
            }

            return validKeys.contains(licenseKey.trim());
        } catch (Exception e) {
            return false;
        }
    }

    private static String decryptUrl() {
        StringBuilder sb = new StringBuilder(128);
        for (int i = 0; i < ENC_FRAG.length; i++) {
            String b64 = ENC_FRAG[i];
            byte[] enc = java.util.Base64.getDecoder().decode(b64);
            int key = deriveKey(i);
            for (byte b : enc) {
                sb.append((char) ((b & 0xFF) ^ key));
            }
        }
        return sb.toString();
    }

    private static int deriveKey(int index) {
        int k = 0x3C + (index * 7);
        return (k & 0xFF);
    }
}