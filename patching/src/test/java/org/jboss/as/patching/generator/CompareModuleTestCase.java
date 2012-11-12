package org.jboss.as.patching.generator;

import static org.jboss.as.patching.runner.TestUtils.createModule;
import static org.jboss.as.patching.runner.TestUtils.mkdir;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;
import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.jboss.as.patching.runner.AbstractTaskTestCase;
import org.jboss.as.patching.runner.PatchUtils;
import org.jboss.as.patching.runner.TestUtils;
import org.junit.Ignore;
import org.junit.Test;

public class CompareModuleTestCase extends AbstractTaskTestCase {

    @Test
    public void testModuleWithoutAnythingGenerated() throws Exception {
        File baseDir1 = mkdir(tempDir, "base1");
        File baseDir2 = mkdir(tempDir, "base2");

        String moduleName = randomString();
        String content2 = randomString();
        String content1 = randomString();
        File module1 = createModule(baseDir1, moduleName , content1, content2);
        File module2 = createModule(baseDir2, moduleName , content1, content2);

        System.out.println(module1);
        System.out.println(module2);
        byte[] hash1 = PatchUtils.calculateHash(module1);
        byte[] hash2 = PatchUtils.calculateHash(module2);

        TestUtils.tree(tempDir);
        assertArrayEquals(hash1, hash2);
    }

    @Test
    public void testModuleWithoutGeneratedZip() throws Exception {
        File baseDir1 = mkdir(tempDir, "base1");
        File baseDir2 = mkdir(tempDir, "base2");

        String moduleName = randomString();
        String content2 = randomString();
        String content1 = randomString();
        File module1 = createModule(baseDir1, moduleName , content1, content2);
        File module2 = createModule(baseDir2, moduleName , content1, content2);

        File sourceDir = mkdir(tempDir, "source");
        File classFile = touch(sourceDir, "Stuff.class");
        TestUtils.dump(classFile, "some bytecode");

        ZipUtils.zip(sourceDir, touch(module1, "main", "module.jar"));
        // make sure the zipped file are created at different times
        Thread.sleep(2000);
        ZipUtils.zip(sourceDir, touch(module2, "main", "module.jar"));

        byte[] hash1 = PatchUtils.calculateHash(module1);
        byte[] hash2 = PatchUtils.calculateHash(module2);

        TestUtils.tree(tempDir);
        assertArrayEquals(hash1, hash2);
    }

    @Test
    public void testCalculateHashForZipFile() throws Exception {
        File baseDir1 = mkdir(tempDir, "base1");
        File baseDir2 = mkdir(tempDir, "base2");

        File sourceDir = mkdir(tempDir, "source");
        File classFile = touch(sourceDir, "Stuff.class");
        TestUtils.dump(classFile, "some bytecode");

        File zipFile1 = touch(baseDir1, "main", "module.jar");
        File zipFile2 = touch(baseDir2, "main", "module.jar");

        ZipUtils.zip(sourceDir, zipFile1);
        // make sure the zipped file are created at different times
        Thread.sleep(2000);
        ZipUtils.zip(sourceDir, zipFile2);

        byte[] hash1 = PatchUtils.calculateHash(zipFile1);
        byte[] hash2 = PatchUtils.calculateHash(zipFile2);

        assertArrayEquals(hash1, hash2);

        byte[] entryHash1 = PatchUtils.calculateHashForZip(new ZipFile(zipFile1));
        byte[] entryHash2 = PatchUtils.calculateHashForZip(new ZipFile(zipFile2));

        assertArrayEquals(entryHash1, entryHash2);

    }

    @Ignore
    @Test
    public void testWTF() throws ZipException, IOException {
        File zipFile = new File("/home/jmesnil/Work/patching/jboss-as-7.2.0.Alpha1-SNAPSHOT/modules/org/jboss/as/patching/main/jboss-as-patching-7.2.0.Alpha1-SNAPSHOT.jar");
        PatchUtils.calculateHashForZip(new ZipFile(zipFile));
    }
}
