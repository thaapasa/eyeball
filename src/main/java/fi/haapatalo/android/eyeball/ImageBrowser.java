package fi.haapatalo.android.eyeball;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageBrowser {

    private static final String TAG = "ImageBrowser";
    private static final File DIR = new File("/sdcard/Pictures/Eyeball");

    private final ImageLoader loader;
    private int index = 0;

    public interface ImageLoader {
        void load(File image);
    }

    public ImageBrowser(ImageLoader loader) {
        this.loader = loader;
    }

    public void next() {
        index += 1;
        load();
    }

    public void previous() {
        index -= 1;
        load();
    }

    public void load() {
        File image = getImage();
        Log.i(TAG, "Loading image " + index + ": " + image);
        loader.load(image);
    }

    public File getImage() {
        List<File> files = getImages();
        if (files.isEmpty()) {
            Log.w(TAG, "No images in " + DIR);
            return null;
        }
        index = bound(index, files.size());
        return files.get(index);
    }

    private static int bound(int val, int max) {
        while (val < 0) { val += max; }
        while (val >= max) { val -= max; }
        return val;
    }

    private static final FileFilter notDirectoryFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return !pathname.isDirectory();
        }
    };

    private static final List<File> noFiles = new ArrayList<>();

    private List<File> getImages() {
        Log.i(TAG, "Loading images from " + DIR);
        if (DIR.isDirectory()) {
            if (!DIR.canRead()) {
                Log.w(TAG, "Cannot read image directory");
            }
            File[] images = DIR.listFiles(notDirectoryFilter);
            return images != null ? Arrays.asList(images) : noFiles;
        } else return noFiles;
    }
}
