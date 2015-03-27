package com.neverfail;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

import static javax.xml.bind.DatatypeConverter.parseHexBinary;
import static javax.xml.bind.DatatypeConverter.printHexBinary;


class NeverFail {
    private static final byte COMPRESSION_NONE = 'F';
    private static final byte COMPRESSION_ZIP = 'C';
    private static final byte COMPRESSION_LZMA = 'Z';
    private byte compression = COMPRESSION_NONE;

    /**
     * Main constructor, start neverFail process
     *
     * @param quiz A valid quiz file
     * @throws Exception
     */
    private NeverFail(File quiz) throws Exception {

        // validate quiz file
        File swf = checkQuiz(quiz);
        // uncompressed it (if needed)
        byte[] rawBytes = this.decompressSwf(swf);

        // search for functions entry/end points
        this.searchFunctionsPositions(rawBytes);

        // search for variables addresses
        this.searchAddressesDirty(rawBytes);

        // default behavior, we just pick a random mark
        this.updateRandomRange(50, 80);

        // use variables addresses map to update bytecode
        this.updateVariablesAddresses();

        // rewrite bytecode
        this.insertHack(rawBytes);

        // finally write all changes to swf
        this.writeSwf(swf, rawBytes);
    }

    public static void main(String[] args) throws Exception {
        // add swag because we are cool
        // this look&feel is available at https://github.com/bulenkov/Darcula
        try {
            UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");
        } catch (Exception ignored) {
        } // it's okay to look ugly }

        try {
            // quiz path is send by args, or current path, or we prompt for it
            // we allow a folder or .zip or .swf file
            if (args.length > 0) {
                new NeverFail(new File(args[0]));
            } else {
                new NeverFail(new File("."));
            }

            // everything went fine. Display ok popup.
            JOptionPane.showMessageDialog(null, "Cheat Worked !", "NeverFail", JOptionPane.INFORMATION_MESSAGE);

        } catch (InvalidQuizFile err) { // quiz invalid, prompt user to enter correct path and retry
            main(new String[]{quizPrompt()});
        } catch (Exception err) { // display error in a popup
            JOptionPane.showMessageDialog(null, "An error occurred:\n" + err.getMessage(), "NeverFail - Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Shows up an interface to pick a file or folder containing a quiz
     *
     * @return picked file or folder
     * @throws Exception when no choices are made
     */
    private static String quizPrompt() throws Exception {
        // display prompt
        int retry = 3; // after 3 failed prompt, we give up
        String path = null;
        while (path == null && retry-- > 0) {
            // prompt for directory
            JFileChooser jfc = new JFileChooser();
            jfc.setDialogTitle("Select quiz directory");
            jfc.setDragEnabled(true);
            jfc.setControlButtonsAreShown(true);
            if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                path = jfc.getSelectedFile().getAbsolutePath();

                // check chosen file is a quiz
                try {
                    NeverFail.checkQuiz(new File(path));
                } catch (InvalidQuizFile err) {
                    // ignore this, simply reprompt
                }
            }
        }

        if (retry < 0) { // After 3 fail, throw error
            JOptionPane.showMessageDialog(null, "You must select a folder containing a quiz.", "NeverFail - Error", JOptionPane.ERROR_MESSAGE);
            throw new Exception();
        }

        return path;
    }

    /**
     * Search path for a quiz, uncompressed it if needed and return main swf
     *
     * @param quiz A path or file designating a quiz
     * @return Main swf file
     */
    private static File checkQuiz(File quiz) throws InvalidQuizFile {
        try {
            // file must exist (yeah no shit sherlock...)
            if (!quiz.exists()) throw new FileNotFoundException("Quiz file given does not exist");

            // it can be a file
            if (quiz.isFile()) {
                // it we gave a zip, extract it, then recheck from extracted data
                if (quiz.getName().toLowerCase().endsWith("zip")) {
                    File outputDir = new File(quiz, quiz.getName().substring(0, quiz.getName().length() - 4));
                    CompressionUtils.extract(quiz, outputDir);
                    return checkQuiz(outputDir);
                }
                // if we have anything else but a swf, error
                else if (!quiz.getName().endsWith("swf")) {
                    throw new InvalidQuizFile("Given file must be a swf");
                }
                // swf is fine, ensure we can read/write and that it's not empty
                else {
                    if (!quiz.canRead()) throw new Exception("Quiz file is not readable");
                    if (!quiz.canWrite()) throw new Exception("Quiz file is not writable");
                    if (Files.size(quiz.toPath()) < 1) throw new Exception("Quiz file is empty (0 bytes)");
                    return quiz;
                }
            }

            // or a directory
            else if (quiz.isDirectory()) {
                // recursive search for quiz.swf then recheck
                File trySwf = new File(quiz, "quiz.swf");
                if (trySwf.exists()) return checkQuiz(trySwf);

                File[] files = quiz.listFiles();
                if (files != null) {
                    for (File dir : files) {
                        if (dir.isDirectory()) {
                            return checkQuiz(dir);
                        }
                    }
                } else {
                    throw new InvalidQuizFile("No quiz found inside directory");
                }

                /* Java 1.8 is better, but less accepted so ...

                final Stream<Path> pathSwfFiles = Files.find(quiz.toPath(), 3, new BiPredicate<Path, BasicFileAttributes>() {
                    @Override
                    public boolean test(Path path, BasicFileAttributes basicFileAttributes) {
                        return path.getFileName().toString().equals("quiz.swf");
                    }
                });
                final Optional<Path> first = pathSwfFiles.findFirst();
                if(!first.isPresent()) throw new FileNotFoundException("No quiz.swf file found inside provided path");
                return checkQuiz(first.get().toFile());
                */
            }
        } catch (InvalidQuizFile err) {
            throw err;
        } catch (Exception err) { // encapsulate others exception so we only return invalidQuizFile exceptions
            throw new InvalidQuizFile(err);
        }

        // does that even make sense ?
        throw new InvalidQuizFile("This error should never be triggered, means we haven't find a problem with your file, but it's not a quiz.");
    }

    /**
     * Open swf file and decompress body if required
     *
     * @return Full swf uncompressed bytes
     */
    private byte[] decompressSwf(File swf) throws Exception {
        Logger.log("Decompressing Swf file");
        try {
            // create stream for swf file
            try (FileInputStream fis = new FileInputStream(swf)) {
                byte[] header = new byte[8];
                byte[] body = new byte[(int) swf.length() - 8];

                // read swf header and body
                try {
                    new DataInputStream(fis).readFully(header);
                    new DataInputStream(fis).readFully(body);
                } catch (Exception err) {
                    Logger.warn("Swf body seems corrupted");
                }

                // prepare output
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    compression = header[0];
                    header[0] = COMPRESSION_NONE; // ensure we always return uncompressed data
                    outputStream.write(header);

                    // decompressed body
                    switch (compression) {
                        case COMPRESSION_NONE:
                            outputStream.write(body);
                            break;
                        case COMPRESSION_ZIP:
                            outputStream.write(CompressionUtils.inflate(body));
                            break;
                        case COMPRESSION_LZMA:
                            outputStream.write(CompressionUtils.LZMAinflate(body));
                            break;
                        default:
                            throw new Exception("Unsupported swf compression");
                    }
                    // return decompressed swf bytes
                    return outputStream.toByteArray();
                }
            }
        } catch (Exception err) {
            throw new Exception("Unable to uncompressed swf", err);
        }
    }

    /**
     * Search offsets for required functions
     *
     * @param rawBytes Swf to search in
     * @throws Exception
     */
    private void searchFunctionsPositions(byte[] rawBytes) throws Exception {
        // search main entry point
        final MagicConstants.Function entryFunction = MagicConstants.FUNCTIONS[0];
        final int index = KPM.indexOf(rawBytes, entryFunction.startSignature);
        if (index < 0) {
            throw new Exception("Can't find initial entry point, unable to hack");
        }
        entryFunction.startOffset = index;

        // cut bytes array to speedup search
        final byte[] rawBytesSmaller = Arrays.copyOfRange(rawBytes, index + 1, index + 500);


        // search entry points and end points offsets
        for (MagicConstants.Function func : MagicConstants.FUNCTIONS) {
            if (func != entryFunction) { // we already found entry offset
                int subindex = KPM.indexOf(rawBytesSmaller, func.startSignature);
                if (subindex < 0) {
                    throw new Exception("Can't find entry point for function " + func.name + "()");
                }
                func.startOffset = index + subindex + 1;
            }

            int subindex = KPM.indexOf(rawBytesSmaller, func.endSignature);
            if (subindex < 0) {
                throw new Exception("Can't find end point for function " + func.name + "()");
            }
            //noinspection ConstantConditions
            func.endOffset = index + subindex + func.endSignature.length + 1;
        }
    }

    /**
     * Search variables addresses base on code signature
     *
     * @param rawBytes Swf to search in
     */
    private void searchAddressesDirty(byte[] rawBytes) throws Exception {
        for (MagicConstants.Variable variable : MagicConstants.VARIABLES) {
            variable.searchAddress(rawBytes);
        }
    }

    /**
     * Replace mapped variables addresses into bytecode
     */
    private void updateVariablesAddresses() {
        // pretty much strait forward
        for (MagicConstants.Variable variable : MagicConstants.VARIABLES) {
            for (MagicConstants.Function func : MagicConstants.FUNCTIONS) {
                func.bytecode = func.bytecode.replace("%" + variable.name + "%", printHexBinary(variable.address));
            }
        }
    }

    /**
     * Update bytecode to manipulate random score value
     * Formula is: random() * range1 + range2
     *
     * @param range1 multiplier
     * @param range2 additional value
     * @throws Exception
     */
    private void updateRandomRange(int range1, int range2) throws Exception {
        if (range1 < 0 || range1 > range2 || range2 > 100) {
            throw new Exception("Range must be between 0-100 ");
        }

        String range1str = Integer.toHexString(range2 - range1);
        String range2str = Integer.toHexString(range1);

        for (MagicConstants.Function func : MagicConstants.FUNCTIONS) {
            // update bytecode, ensure hex number is 0 padded (0x01 not 0x1)
            func.bytecode = func.bytecode.replace("%range1%", (range1str.length() < 2) ? "0" + range1str : range1str);
            func.bytecode = func.bytecode.replace("%range2%", (range2str.length() < 2) ? "0" + range2str : range2str);
        }
    }

    /**
     * Write new function info swf bytes array
     *
     * @param rawBytes swf byte array to update
     * @throws Exception
     */
    private void insertHack(byte[] rawBytes) throws Exception {
        for (MagicConstants.Function func : MagicConstants.FUNCTIONS) {
            // check bytecode does not contain replacement pattern
            if (func.bytecode.contains("%")) {
                throw new Exception(func.name + "() bytecode sequence contain unmodified pattern");
            }

            // insert hack and 0x02 (Nop) instruction for remaining bytes
            int funcLength = func.endOffset - func.startOffset;
            byte[] opCodes = parseHexBinary(func.bytecode);
            for (int i = 0; i < funcLength; i++) {
                rawBytes[func.startOffset + i] = (i < opCodes.length) ? opCodes[i] : 0x02;
            }
            rawBytes[func.startOffset - 5] = func.maxStack;
        }
    }

    /**
     * write bytes array to specified file
     *
     * @param swf      file to write on
     * @param rawBytes bytes array to write
     * @throws Exception when shit happen
     */
    private void writeSwf(File swf, byte[] rawBytes) throws Exception {
        try {
            // in case original file was compressed, we do deflate
            // java deal with the writing like a boss
            Files.write(swf.toPath(), compressSwf(rawBytes));
        } catch (IOException err) {
            throw new Exception("Unable to write swf file", err);
        }
    }

    /**
     * Try compressing swf bytes data like original or return uncompressed data
     *
     * @param rawBytes swf bytes uncompressed
     * @return swf bytes compressed (or not)
     */
    private byte[] compressSwf(byte[] rawBytes) {
        // todo: compression don't work, need a debug,
        // todo: since then, compression is set to none
        compression = COMPRESSION_NONE;


        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // write header
            outputStream.write(rawBytes, 0, 8);

            // prepare body for compression
            byte[] body = Arrays.copyOfRange(rawBytes, 8, rawBytes.length);
            switch (compression) {
                case COMPRESSION_ZIP:
                    // compress
                    outputStream.write(CompressionUtils.deflate(body));
                    byte[] compressedBytes = outputStream.toByteArray();
                    compressedBytes[0] = COMPRESSION_ZIP;
                    return compressedBytes;
                case COMPRESSION_LZMA:
                    outputStream.write(CompressionUtils.LZMAdeflate(body));
                    compressedBytes = outputStream.toByteArray();
                    compressedBytes[0] = COMPRESSION_LZMA;
                    return compressedBytes;
            }
        } catch (IOException ignored) {
        } // ignore, we just send rawByte uncompressed
        // whatever happened, we return rawbytes
        return rawBytes;
    }

    /**
     * Exception thrown when a file does not match the prerequisites
     * to be a quiz
     */
    private static class InvalidQuizFile extends Exception {
        public InvalidQuizFile(Throwable err) {
            super(err);
        }

        public InvalidQuizFile(String s) {
            super(s);
        }
    }
}
