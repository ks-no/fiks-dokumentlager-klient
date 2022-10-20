package no.ks.fiks.dokumentlager.klient;

public class EncryptUtil {

    public static final String PUBLIC_KEY =
            "-----BEGIN CERTIFICATE-----\n" +
                    "MIIEIDCCAwigAwIBAgIJAOfdsbcJ9VCaMA0GCSqGSIb3DQEBCwUAMIGjMQswCQYD\n" +
                    "VQQGEwJOTzEWMBQGA1UECAwNRG9rdW1lbnRsYWdlcjEWMBQGA1UEBwwNRG9rdW1l\n" +
                    "bnRsYWdlcjEWMBQGA1UECgwNRG9rdW1lbnRsYWdlcjEWMBQGA1UECwwNRG9rdW1l\n" +
                    "bnRsYWdlcjEWMBQGA1UEAwwNRG9rdW1lbnRsYWdlcjEcMBoGCSqGSIb3DQEJARYN\n" +
                    "RG9rdW1lbnRsYWdlcjAgFw0xOTAxMjIwNzExNDlaGA80NzU2MTIxODA3MTE0OVow\n" +
                    "gaMxCzAJBgNVBAYTAk5PMRYwFAYDVQQIDA1Eb2t1bWVudGxhZ2VyMRYwFAYDVQQH\n" +
                    "DA1Eb2t1bWVudGxhZ2VyMRYwFAYDVQQKDA1Eb2t1bWVudGxhZ2VyMRYwFAYDVQQL\n" +
                    "DA1Eb2t1bWVudGxhZ2VyMRYwFAYDVQQDDA1Eb2t1bWVudGxhZ2VyMRwwGgYJKoZI\n" +
                    "hvcNAQkBFg1Eb2t1bWVudGxhZ2VyMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB\n" +
                    "CgKCAQEAozKOfBKRoU7AgRAfvROwAKGPuZOyp5x7WB0pZZca7xhB01k0CZGsPr42\n" +
                    "6B1MtufuDlbMEBnUsbuPGrU5jZ02OOcITXa9t8t4GF0UnwffYa9Jn1GewTYzP0oo\n" +
                    "rNCXMJyzsZOVUSOvctG/X5z8i5TZs9gtSYun0rvBqENKGJZubx67aTtABfAuDioY\n" +
                    "xsW0KBt2LuhrcykoH9hJYdPBvS8PuCAIzhXxWG/VEHAnS+x4jpR7UkKt3yGtRa8s\n" +
                    "OZ94xosXjNj6vAtb1TpvcfZV/9E2bxJtUVIPaAS2jt2Qo0pc6ea05MSSxsl574am\n" +
                    "J/F9nQ9FMs6t9ZIeBdU95abu8rOxOwIDAQABo1MwUTAdBgNVHQ4EFgQUHnsFG7Kx\n" +
                    "IOwibkHTnBwY79jbjKkwHwYDVR0jBBgwFoAUHnsFG7KxIOwibkHTnBwY79jbjKkw\n" +
                    "DwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAGBJOnx1zOZYbwqxG\n" +
                    "iGbR98ms2OydjUBbaiB9SneWomTXGXSI3j/7xUlCyQFLQiivUI2Ip5x0nhPMOaYK\n" +
                    "yTYy6rZ/geBmcOpWihd4LnNLO3AT2dYcptdG213yIomjRk4BNaAQCMt9ZcicTzxG\n" +
                    "eV1oa2ERdRt+y8fkVd0QZ6lhXOssW2vMt9AC2k4LL9woJrgZs4CvtCDKHET+HvvI\n" +
                    "7NbuaTZSNolZwR5hIdtq8nKCvNVp4VvOFgT8WIuudMx1tWgDIo9ttLCV7tz9WjtL\n" +
                    "L9fbdxYO5UGayVq0IFt4gCQLpkcaThaqRVeC+l7PU7WHHqrUzsnSQpm8Hp40D2zI\n" +
                    "dkMkSA==\n" +
                    "-----END CERTIFICATE-----";

    public static final String PRIVATE_KEY =
            "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCjMo58EpGhTsCB\n" +
                    "EB+9E7AAoY+5k7KnnHtYHSlllxrvGEHTWTQJkaw+vjboHUy25+4OVswQGdSxu48a\n" +
                    "tTmNnTY45whNdr23y3gYXRSfB99hr0mfUZ7BNjM/Siis0JcwnLOxk5VRI69y0b9f\n" +
                    "nPyLlNmz2C1Ji6fSu8GoQ0oYlm5vHrtpO0AF8C4OKhjGxbQoG3Yu6GtzKSgf2Elh\n" +
                    "08G9Lw+4IAjOFfFYb9UQcCdL7HiOlHtSQq3fIa1Fryw5n3jGixeM2Pq8C1vVOm9x\n" +
                    "9lX/0TZvEm1RUg9oBLaO3ZCjSlzp5rTkxJLGyXnvhqYn8X2dD0Uyzq31kh4F1T3l\n" +
                    "pu7ys7E7AgMBAAECggEALKBJiDIHsqV3TJOdKjX0/ecwBx4VT3Ih5HFs/YO5cMIg\n" +
                    "Vevhp/A2up2HJCfG74kydqdTe9+kYsmYE0SVLV1dE2hRw+UBcf3opDjnx6j+c5bc\n" +
                    "Of22vLzWfKsJvl/3x+pB1QA3Z42rj2k9vKaQBJc6hMxLbf4LcTu4dAuaemjAYBAG\n" +
                    "gXoBeG5m7APYEaGIbGdl8UEWMf/GrarDMAYMWoatKIRkhwYVazfjaoVxk2kybCA0\n" +
                    "RjsGjrXTETojKmFi2ImQYC9VVdOSGBlqTfuJHv3MaWOD09W3/IfRrMWag0UrX1zB\n" +
                    "T4IXnZFl+1AHVJ6AXC6114mW1hFJuxGnAlXbEHUJOQKBgQDPN0gvU5l/MD1UzL8Y\n" +
                    "kyiJtE6fjXo2Z0mcwXv/ZocKq/BvuEjbav4QzmF/6oiC9zpgmbvxsZpjyz8vkqc6\n" +
                    "vDTmE07Bkp+bxXXI821KONLCsIyRxDXT7JSyRWCoD0TAEX/IkYh6GWAxqtnzaTQi\n" +
                    "gqelHI/oId+fIp6K9UHBGAW7JQKBgQDJnlJjpJWEcvLOjhST3m7eeGotrR2SO/U9\n" +
                    "+nouLnGglDpEp8UbvLbTPLAoYBRaFLxr95quCh/+96f2wmrcfWGt+K4K3LKtDZbu\n" +
                    "0Clg4RuhZWokTJ7QNpKgTGmQCsboMM9AJTBPQCj2uESb7+V/LVTPzyvs9U7RgaRn\n" +
                    "8YLqI6U83wKBgBWbnCljPFRpAVxAZYT4g3eol7JHnIDj0GdKPdXqKRbRyya7Ps2y\n" +
                    "oH+8JaqjGE0f3rSIE3MmpATYAuTBFDMpwRJk3QeOdJpXwuqLh8//kOrAYkgo/7vz\n" +
                    "paXZWjTsMq0cpgiSNHsW/lLvj/6z773Rhg3Ppqn8Lkd34rR20r6B9McJAoGAH+JF\n" +
                    "rTRN4NA8zaVyY5/9cHkicW67CnEo61A9GiiGF5rZTBor9aL2Vpl2Uiw/i69TzM8v\n" +
                    "Su6W+L85dLByLcQ2OkjlXRphtzQ69jE9GfD/aZqcGnlzdAHtViQ/XWQW6IkvfTlk\n" +
                    "VmQTFlE1qGNbq60DiIl+rM5uVHtoAHgU9+oDK4kCgYBwAD1SPW7b3OKoleyjUew6\n" +
                    "FoJ952ShhEeYmyMpwhYayUY1SEdcjJlxDj5RyVCvWq2xu7LL/7uLRaQR9XiLLwxE\n" +
                    "m+q8ASYbc1Fh8UykUNFcOKqTImWA0CGuDndc1ZXxwA9SN9/UNap21dfZrJN/VhBN\n" +
                    "Ad1DLPxU/e3rN6lr/Yopqw==\n";

}
