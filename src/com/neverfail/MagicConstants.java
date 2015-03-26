package com.neverfail;

import java.util.Arrays;

import static javax.xml.bind.DatatypeConverter.parseHexBinary;

/**
 * These constant are used to match data into actionScript bytecode
 * They are given as it and may not work if the data signature change
 * Read bytecode.txt for more information
 */

@SuppressWarnings("UnusedDeclaration")
class MagicConstants {
    public final static Function[] FUNCTIONS = new Function[]{
            new Function() {{
                startSignature = parseHexBinary("d0302400d62085d720");
                endSignature = parseHexBinary("15beffffd148");

                name = "Passed";
                bytecode =
                      "d030d066%m_nScore%24000c430000d066%m_nScore%9082d5d0240061%m_nScore%2400d61"
                    + "023000009d0d066%m_nScore%d066%m_arrInteractions%d2752a9182d666%{}%66%MaxSco"
                    + "re%a061%m_nScore%d066%m_nScore%d1170c0000d2d066%m_arrInteractions%66%length"
                    + "%15d1ffff2648";

                maxStack = 0x0e;
            }},
            new Function() {{
                startSignature = parseHexBinary("d0302400d620");
                endSignature = parseHexBinary("75d5d148");

                name = "Score";
                bytecode =
                      "d030d066%m_nScore%d066%PassScore%0c1f0000d060%Math%46%random%0024%range1%"
                    + "a224%range2%a0d066%MaxScore%a22464a39061%m_nScore%d066%Passed%d5d066%m_nScore%48";

                maxStack = 0x0e;
            }}
    };

    public static final Variable[] VARIABLES = new Variable[] {
            new Variable("m_arrInteractions",   "2400d61032000009d066****"),
            new Variable("length",              "100f000009d22400d266****"),
            new Variable("MaxScore",            "****a325904e"),
            new Variable("PassScore",           "****b0d5"),
            new Variable("Math",                "D6101F000060****"),
            new Variable("random",              "1023000009620562041e85d7d166****"),
            new Variable("Passed",              "112c0000d166****"),
            new Variable("{}",                   "****8001d624006304"),
            new Variable("m_nScore",            "1268000060") {
                @Override
                public void searchAddress(byte[] rawBytes) throws Exception {
                    // m_nScore is annoying so we handle it manually
                    int index = KPM.indexOf(rawBytes, parseHexBinary("1268000060"));
                    address = Arrays.copyOfRange(rawBytes, index - 6, index - 4);
                }
            }
    };


    public static abstract class Function {
        /**
         * Match the beginning of the function
         */
        public byte[] startSignature = null;

        /**
         * Match the end of the function
         */
        public byte[] endSignature = null;

        /**
         * Function name
         */
        public String name = null;

        /**
         * Raw bytecode to insert, some variables will be replaced for it to work
         * To learn more about that, read bytecodes.txt
         */
        public String bytecode = null;

        /**
         * Offset representing the beginning of the function
         */
        public int startOffset;

        /**
         * Offset representing the end of the function
         */
        public int endOffset;

        /**
         * Value of the maxStack attribute to set
         */
        public byte maxStack;
    }

    public static class Variable {
        public final String pattern;
        public final String name;
        public byte[] address;

        public Variable(String name, String pattern) {
            this.pattern = pattern;
            this.name = name;
        }

        public void searchAddress(byte[] rawBytes) throws Exception {
            boolean reverse = (pattern.indexOf("*") == 0);
            String searchpattern = pattern.replace("*", "");


            int index = KPM.indexOf(rawBytes, parseHexBinary(searchpattern));
            if(index > -1) {
                if(!reverse) {
                    address = Arrays.copyOfRange(rawBytes, index + searchpattern.length() / 2, index + 2 + searchpattern.length() / 2);
                } else {
                    address = Arrays.copyOfRange(rawBytes, index - 2, index);
                }
            } else {
                throw new Exception("Can't find address value of variable '" + name + "'");
            }
        }
    }
}
