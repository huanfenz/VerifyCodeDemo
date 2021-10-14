package com.wangpeng.demo1;

import redis.clients.jedis.Jedis;

import java.util.Random;
import java.util.Scanner;

/**
 * 思路：
 * 1. 输入手机号
 * 2. 判断该手机号的发送次数，满了返回失败，没满count++并放置验证码到redis中
 * 3. 发短信，把验证码发给目标手机
 * 4. 用户输入验证码，与redis中的验证码进行比较，输出成功与否
 */

public class VerifyCode {
    public static void main(String[] args) {
        //输入手机号
        System.out.print("请输入手机号:");
        Scanner sc = new Scanner(System.in);
        String phone = sc.next();

        //放置验证码
        String code = setCode(phone);
        if(code == null) {  //没成功放置，说明满了3次
            System.out.println("今日已发送3次，请明天再尝试");
            return;
        }

        //发送短信
        System.out.println("发送短信,您的验证码是:" + code);

        //等待用户输入验证码
        System.out.print("请输入验证码:");
        String userCode = sc.next();

        //得到验证码进行校验
        String redisCode = getCode(phone);
        if(redisCode == null) { //没成功得到，说明验证码超时
            System.out.println("验证码超时，请重新获取");
            return;
        }
        if(userCode.equals(redisCode)) {
            System.out.println("恭喜你，验证码正确");
        } else {
            System.out.println("很抱歉，验证码错误");
        }
    }

    private static String getCode(String phone) {
        //连接redis
        Jedis jedis = new Jedis("47.97.104.230", 6391);
        jedis.auth("7783772Wangpeng?");

        String codeKey = "VerifyCode" + phone + ":code";

        // 获得验证码
        String code = jedis.get(codeKey);

        jedis.close();
        return code;
    }

    /**
     * 得到验证码
     * @param phone
     * @return
     */
    private static String setCode(String phone) {
        //连接redis
        Jedis jedis = new Jedis("47.97.104.230", 6391);
        jedis.auth("7783772Wangpeng?");

        String countKey = "VerifyCode" + phone + ":count";
        String codeKey = "VerifyCode" + phone + ":code";

        //得到次数
        String countStr = jedis.get(countKey);
        if(countStr == null) { //当前是0次
            jedis.setex(countKey, 24 * 60 * 60,"1"); //设置1次，时间24小时
        } else if(Integer.parseInt(countStr) <= 2) { //当前小于2次
            jedis.incr(countKey);
        } else {    //否则大于2次，不能发了
            jedis.close();  //返回前关闭redis
            return null;
        }

        String verifyCode = createCode();
        //放进redis
        jedis.setex(codeKey, 60 * 2, verifyCode); //自动覆盖

        jedis.close();  //返回前关闭redis
        return verifyCode;
    }

    /**
     * 创建一个验证码
     * @return 验证码
     */
    private static String createCode() {
        //取5位随机数,10000~99999
        Random random = new Random();
        Integer randomNumb = random.nextInt(89999) + 10000;
        return randomNumb.toString();
    }
}
