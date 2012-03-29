package org.jzy3d.global;

import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.jvm.JNILibLoaderBase;
import com.jogamp.common.util.cache.TempJarCache;

import javax.media.nativewindow.NativeWindowFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;

/**
 * Global static class to activate:<ol>
 *     <li>com.jogamp.common.util.cache.TempJarCache#bootstrapNativeLib(java.lang.Class<?>, java.lang.String, java.net.URL, java.lang.ClassLoader)</li>
 *     <li>AND THEN</li>
 *     <li>com.jogamp.common.os.DynamicLibraryBundle.GlueJNILibLoader#loadLibrary(java.lang.String, boolean)</li>
 * </ol>
 * correctly from jar Gradle dependency resolution.
 *
 * @author Fred Simon
 */
public class NativeLibraryLoader {
    public static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    public static final String ARCH_NAME = System.getProperty("os.arch").toLowerCase();

    public static void loadLibraries() {
        loadLibrary("gluegen-rt", JogampRuntimeException.class, false);
        loadLibrary("jogl_desktop", NativeWindowFactory.class, true);
    }

    private static void loadLibrary(final String libBaseName, final Class<?> classInJar, final boolean loadAll) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                if (TempJarCache.initSingleton()) {
                    try {
                        // Looking in the classloader and depending on the OS and ARCH find the jar containing the lib
                        URL nativeJarURL = null;
                        final ClassLoader cl = classInJar.getClassLoader();
                        String libFileName = null;
                        String jarNameShouldContains = null;
                        if (OS_NAME.startsWith("linux")) {
                            libFileName = "lib" + libBaseName + ".so";
                            if (ARCH_NAME.equals("x86_64") || ARCH_NAME.equals("amd64")) {
                                jarNameShouldContains =  "linux64";
                            } else {
                                jarNameShouldContains =  "linux32";
                            }
                        } else if (OS_NAME.startsWith("mac os x") ||
                                OS_NAME.startsWith("darwin")) {
                            libFileName = "lib" + libBaseName + ".jnilib";
                            jarNameShouldContains = "macosx";
                        }
                        if (libFileName != null) {
                            Enumeration<URL> resources = cl.getResources(libFileName);
                            while (resources.hasMoreElements()) {
                                URL url = resources.nextElement();
                                URL jarUrl = extractJarUrl(url, jarNameShouldContains);
                                if (jarUrl != null) {
                                    nativeJarURL = jarUrl;
                                    break;
                                }
                            }
                            if (nativeJarURL != null) {
                                if (loadAll) {
                                    TempJarCache.addNativeLibs(classInJar, nativeJarURL, cl);
                                } else {
                                    TempJarCache.bootstrapNativeLib(classInJar, libBaseName, nativeJarURL, cl);
                                }
                            }
                        }
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                GlueJNILibLoader.loadLibrary(libBaseName, false);
                return null;
            }
        });
    }

    /** Inherit access */
    static class GlueJNILibLoader extends JNILibLoaderBase {
        protected static synchronized boolean loadLibrary(String libname, boolean ignoreError) {
            return JNILibLoaderBase.loadLibrary(libname, ignoreError);
        }
    }
    
    private static URL extractJarUrl(URL resourceUrl, String shouldContains) throws MalformedURLException {
        String urlS = resourceUrl.toExternalForm();
        if(!urlS.startsWith("jar:")) {
            throw new IllegalArgumentException("JAR URL doesn't start with 'jar:', got <"+urlS+">");
        }
        urlS = urlS.substring(4, urlS.length()); // exclude 'jar:'

        // from
        //   file:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class
        // to
        //   file:/some/path/gluegen-rt.jar
        int idx = urlS.lastIndexOf('!');
        if (0 <= idx) {
            urlS = urlS.substring(0, idx); // exclude '!/'
        } else {
            throw new IllegalArgumentException("JAR URL does not contain jar url terminator '!', url <"+urlS+">");
        }

        if (!urlS.endsWith(".jar")) {
            throw new IllegalArgumentException("No Jar name in <"+resourceUrl.toExternalForm()+">, got <"+urlS+">");
        }

        if (!urlS.contains(shouldContains)) {
            return null;
        }

        return new URL("jar:"+urlS+"!/");
    }
}
