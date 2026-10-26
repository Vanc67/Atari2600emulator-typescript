package rpc;

public class Compat {
    public Compat() {
    }
    
    public static String why(int i, rpc.Part a, rpc.Part[] a0) {
        if (a == null) {
            String s = null;
            return s;
        }
        rpc.Part a1 = a0[1];
        rpc.Part a2 = a0[0];
        rpc.Part a3 = a0[6];
        rpc.Part a4 = a0[11];
        rpc.Part a5 = a0[13];
        label0: if (i != 0) {
            if (i != 1) {
                label3: {
                    label4: {
                        if (i < 2) {
                            break label4;
                        }
                        if (i < 6) {
                            break label3;
                        }
                    }
                    if (i != 6) {
                        if (i != 13) {
                            label1: {
                                label2: {
                                    if (i < 7) {
                                        break label2;
                                    }
                                    if (i < 10) {
                                        break label1;
                                    }
                                }
                                if (i != 12) {
                                    if (i != 11) {
                                        if (i != 10) {
                                            break label0;
                                        }
                                        if (a4 == null) {
                                            break label0;
                                        }
                                        if (a.f[1] != 0) {
                                            break label0;
                                        }
                                        if ((a4.f[0]) > 2) {
                                            return "AT-\u0431\u043b\u043e\u043a \u043f\u0438\u0442\u0430\u043d\u0438\u044f \u043d\u0435 \u0432\u0441\u0442\u0430\u043d\u0435\u0442 \u0432 ATX-\u043a\u043e\u0440\u043f\u0443\u0441";
                                        }
                                        break label0;
                                    } else {
                                        if (a1 == null) {
                                            break label0;
                                        }
                                        if (!rpc.Score.caseFits(a1.f[4], a.f[0])) {
                                            return new StringBuffer("\u041a\u043e\u0440\u043f\u0443\u0441 ").append(rpc.Score.formName(a.f[0])).append(" \u043c\u0430\u043b \u0434\u043b\u044f \u043f\u043b\u0430\u0442\u044b ").append(rpc.Score.formName(a1.f[4])).toString();
                                        }
                                        break label0;
                                    }
                                } else {
                                    if (a2 == null) {
                                        break label0;
                                    }
                                    if ((a.f[1]) <= 0) {
                                        break label0;
                                    }
                                    if ((a.f[1]) < a2.f[3] / 2) {
                                        return new StringBuffer("\u041a\u0443\u043b\u0435\u0440 \u043d\u0430 ").append(a.f[1]).append(" \u0412\u0442 \u043d\u0435 \u043e\u0445\u043b\u0430\u0434\u0438\u0442 ").append(a2.f[3]).append(" \u0412\u0442").toString();
                                    }
                                    break label0;
                                }
                            }
                            String s0 = rpc.Score.ifaceName(a.f[0]);
                            if (a1 == null) {
                                break label0;
                            }
                            if (s0.equals((Object)"SATA") && a1.year < 2003) {
                                return new StringBuffer("\u041f\u043b\u0430\u0442\u0430 ").append(a1.year).append(" \u0433\u043e\u0434\u0430 \u0435\u0449\u0451 \u043d\u0435 \u0437\u043d\u0430\u0435\u0442 SATA").toString();
                            }
                            if (s0.equals((Object)"NVME") && a1.year < 2015) {
                                return "\u041d\u0443\u0436\u0435\u043d M.2: \u043f\u043b\u0430\u0442\u0430 \u0441\u0442\u0430\u0440\u0435\u0435 2015";
                            }
                            if (s0.equals((Object)"IDE") && a1.year > 2012) {
                                return "\u0423 \u044d\u0442\u043e\u0439 \u043f\u043b\u0430\u0442\u044b \u0443\u0436\u0435 \u043d\u0435\u0442 IDE";
                            }
                            if (!s0.equals((Object)"SCSI")) {
                                break label0;
                            }
                            if (a1.year > 2005) {
                                return "SCSI-\u043a\u043e\u043d\u0442\u0440\u043e\u043b\u043b\u0435\u0440\u0430 \u043d\u0435\u0442 \u043d\u0430 \u043f\u043b\u0430\u0442\u0435";
                            }
                            break label0;
                        } else {
                            if (a3 == null) {
                                break label0;
                            }
                            String s1 = rpc.Ports.join(a, a3);
                            if (s1 != null) {
                                return s1;
                            }
                            break label0;
                        }
                    } else {
                        if (a1 != null && !rpc.Score.busFits(a, a1)) {
                            return new StringBuffer("\u041f\u043b\u0430\u0442\u0430 \u043d\u0435 \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0435\u0442 \u0448\u0438\u043d\u0443 ").append(rpc.Score.busName(a.f[0])).toString();
                        }
                        if (a5 == null) {
                            break label0;
                        }
                        String s2 = rpc.Ports.join(a5, a);
                        if (s2 != null) {
                            return s2;
                        }
                        break label0;
                    }
                }
                if (a1 != null && a.f[0] != 0 != (a1.f[1] != 0)) {
                    return new StringBuffer("\u041f\u043b\u0430\u0442\u0430 \u043f\u043e\u0434 ").append(rpc.Score.ramTypeName(a1.f[1])).append(", \u043c\u043e\u0434\u0443\u043b\u044c \u2014 ").append(rpc.Score.ramTypeName(a.f[0])).toString();
                }
            } else {
                if (a2 != null && a2.f[0] != 0 != (a.f[0] != 0)) {
                    return new StringBuffer("\u041f\u043b\u0430\u0442\u0430 \u043f\u043e\u0434 ").append(rpc.Score.sockName(a.f[0])).append(", \u0430 \u043f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440 \u2014 ").append(rpc.Score.sockName(a2.f[0])).toString();
                }
                if (a4 != null && !rpc.Score.caseFits(a.f[4], a4.f[0])) {
                    return new StringBuffer("\u041f\u043b\u0430\u0442\u0430 ").append(rpc.Score.formName(a.f[4])).append(" \u043d\u0435 \u0432\u043b\u0435\u0437\u0435\u0442 \u0432 \u043a\u043e\u0440\u043f\u0443\u0441 ").append(rpc.Score.formName(a4.f[0])).toString();
                }
                int i0 = 0;
                while(i0 < 4) {
                    rpc.Part a6 = a0[2 + i0];
                    if (a6 != null && a6.f[0] != 0 != (a.f[1] != 0)) {
                        return new StringBuffer("\u0412 \u043f\u043b\u0430\u0442\u0435 ").append(rpc.Score.ramTypeName(a.f[1])).append(", \u0430 \u043f\u0430\u043c\u044f\u0442\u044c ").append(rpc.Score.ramTypeName(a6.f[0])).toString();
                    }
                    i0 = i0 + 1;
                }
                if (a3 != null && !rpc.Score.busFits(a3, a)) {
                    return new StringBuffer("\u041f\u043b\u0430\u0442\u0430 \u043d\u0435 \u0443\u043c\u0435\u0435\u0442 \u0448\u0438\u043d\u0443 \u043a\u0430\u0440\u0442\u044b ").append(rpc.Score.busName(a3.f[0])).toString();
                }
            }
        } else if (a1 != null && a.f[0] != 0 != (a1.f[0] != 0)) {
            return new StringBuffer("\u0421\u043e\u043a\u0435\u0442 \u043d\u0435 \u0442\u043e\u0442: \u043d\u0443\u0436\u0435\u043d ").append(rpc.Score.sockName(a1.f[0])).append(", \u0443 \u043a\u0430\u043c\u043d\u044f ").append(rpc.Score.sockName(a.f[0])).toString();
        }
        String s3 = null;
        return s3;
    }
    
    public static boolean fits(int i, rpc.Part a, rpc.Part[] a0) {
        if (rpc.Compat.why(i, a, a0) != null) {
            return false;
        }
        return true;
    }
}
