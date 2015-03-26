package com.neverfail;

import org.tukaani.xz.SingleXZInputStream;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class CompressionUtils
{


    /***
     * Extract zipfile to outdir with complete directory structure
     * disclaimer, I didn't wrote that ...
     * @param zipfile Input .zip file
     * @param outdir Output directory
     */
    public static void extract(File zipfile, File outdir) {
        // todo: replace with more trustful method
        try
        {
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipfile));
            ZipEntry entry;
            String name, dir;
            while ((entry = zin.getNextEntry()) != null) {
                name = entry.getName();
                if(entry.isDirectory()) {
                    mkdirs(outdir, name);
                    continue;
                }
                dir = dirpart(name);
                if( dir != null ) {
                    mkdirs(outdir, dir);
                }

                extractFile(zin, outdir, name);
            }
            zin.close();
        }
        catch (IOException err)
        {
            throw new RuntimeException("Unable to extract quiz", err);
        }
    }

    private static void extractFile(ZipInputStream in, File outdir, String name) throws IOException {
        byte[] buffer = new byte[4096];

        final File file = new File(outdir, name);
        try(final FileOutputStream fos = new FileOutputStream(file)) {
            try(final BufferedOutputStream out = new BufferedOutputStream(fos)) {
                int count;
                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
            }
        }
    }

    private static void mkdirs(File outdir, String path) {
        File d = new File(outdir, path);
        if(!d.exists()) {
            //noinspection ResultOfMethodCallIgnored
            d.mkdirs();
        }
    }

    private static String dirpart(String name) {
        int s = name.lastIndexOf("/");
        return s == -1 ? null : name.substring(0, s);
    }

    public static byte[] deflate(byte[] bytes) {
        Deflater deflater = new Deflater();
        // compress
        try {
            deflater.setInput(bytes);
            deflater.finish();

            try (ByteArrayOutputStream compressedStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                while (!deflater.finished()) { // read until the end of the stream is found
                    int count = deflater.deflate(buffer); // compress the data into the buffer
                    compressedStream.write(buffer, 0, count);
                }

                return compressedStream.toByteArray();
            }
        } catch (Exception err) {
            Logger.error("Unable to zip swf\n" + err.getMessage());
        } finally {
            deflater.end();
        }
        return bytes;
    }

    public static byte[] inflate(byte[] bytes) {
        Inflater inf = new Inflater();
        try {
            inf.setInput(bytes);

            try (ByteArrayOutputStream uncompressedStream = new ByteArrayOutputStream()) {
                byte[] b = new byte[1024];
                while (!inf.finished()) { //read until the end of the stream is found
                    int count = inf.inflate(b); //decompress the data into the buffer
                    uncompressedStream.write(b, 0, count);
                }

                return uncompressedStream.toByteArray();
            }
        } catch (Exception err) {
            Logger.error("Unable to unzip swf\n" + err.getMessage());
        } finally {
            inf.end();
        }
        return bytes;
    }

    public static byte[] LZMAinflate(byte[] bytes) {
        Logger.warn("LZMA decompression is experimental");
        try (
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                SingleXZInputStream xzInputStream = new SingleXZInputStream(inputStream);
                ByteArrayOutputStream uncompressedStream = new ByteArrayOutputStream()
        ) {
            int n;
            byte[] b = new byte[1024];
            while ((n = xzInputStream.read(b)) > -1) {
                uncompressedStream.write(b, 0, n);
            }

            return uncompressedStream.toByteArray();
        } catch (IOException err) {
            Logger.error("Unable to inflate LZMA swf\n" + err.getMessage());
        }
        return bytes;
    }

    public static byte[] LZMAdeflate(byte[] bytes) {
        // todo: implement
        return bytes;
    }
}