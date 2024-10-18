package com.sapienter.jbilling.server.mediation.chaos;

import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.Set;

public class RuntimeExceptionInjector {

    private static final Set<String> injectForProducts = new HashSet<String>() {{
        add("INJECT_555d299f2351480fa283634bde39447b");
        add("INJECT_07af8383447c4892b6f5f331e13ad643");
        add("INJECT_2817faf1e43d429c9418923d1aeabfce");
        add("INJECT_ffc87b746dbd425f9974056ff5aace44");
        add("INJECT_21a2170b21d44930bbcb0197f8f2fc75");
    }};

    public static void injectError(String productCode, String expected, String desc) {
        if (StringUtils.isBlank(productCode)) {
            return;
        }

        productCode = productCode.trim();
        if (injectForProducts.contains(productCode) && expected.equalsIgnoreCase(productCode)) {
            throw new RuntimeException(
                    String.format("Injecting error: product= %s :: %s",
                    productCode,
                    desc));
        }
    }
}
