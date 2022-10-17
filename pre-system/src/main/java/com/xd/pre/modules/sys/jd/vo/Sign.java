package com.xd.pre.modules.sys.jd.vo;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Sign {

    public static void main(String[] args) throws Exception {
        System.out.println(System.getProperty("os.name"));
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(String.format("node C:\\Users\\Administrator\\Desktop\\唯品会\\node_modules\\xxx.js 1 \"%s\" ", "event=CONVENIENT_LOGIN_WAP_IMG_CAPTCHA&biz_data={\\\"contact_phone\\\":\\\"18572750174\\\",\\\"cid\\\":\\\"1658904923126_6a4ecb6e94c54adece47a5aab56d72ba\\\"}"));
//            pr = rt.exec("dir");//open evernote program
//            Process pr = rt.exec("D:/APP/Tim/Bin/QQScLauncher.exe");//open tim program
        BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream(), "GBK"));
        String line = null;
        StringBuilder lineTotal = new StringBuilder();
        while ((line = input.readLine()) != null) {
            lineTotal.append(line);
        }
        System.out.println(lineTotal.toString());
    }
}
