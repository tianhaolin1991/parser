package com.huawei.fossbot.dependency.dsl.gradle;

import org.apache.commons.lang3.StringUtils;
import java.util.Arrays;

/**
 * 用于标识gradle构建文件路径
 *
 * @author t30002128
 * @since 2020/06/04
 */
public class Path {
    /**
     * ROOT PATH
     */
    public static final Path ROOT = new Path(new String[0], true);
    /**
     * PATH SEPARATOR
     */
    public static final String SEPARATOR = ":";

    /**
     * 获取Path
     */
    public Path path(String path) {
        if (!StringUtils.isEmpty(path)) {
            if (path.equals(SEPARATOR)) {
                return ROOT;
            } else {
                return parsePath(path);
            }
        }
        return null;
    }

    private Path parsePath(String path) {
        String[] seg = StringUtils.split(path, SEPARATOR);
        boolean abs = path.startsWith(SEPARATOR);
        return new Path(seg, abs);
    }

    private final String[] segments;
    private final boolean absolute;
    private String fullPath;

    private Path(String[] segments, boolean absolute) {
        this.segments = segments;
        this.absolute = absolute;
    }

    @Override
    public String toString() {
        return getPath();
    }

    /**
     * Appends the supplied path to this path, returning the new path.
     * The resulting path with be absolute or relative based on the path being appended _to_.
     * It makes no difference if the _appended_ path is absolute or relative.
     *
     * <pre>
     * path(':a:b').append(path(':c:d')) == path(':a:b:c:d')
     * path(':a:b').append(path('c:d')) == path(':a:b:c:d')
     * path('a:b').append(path(':c:d')) == path('a:b:c:d')
     * path('a:b').append(path('c:d')) == path('a:b:c:d')
     * </pre>
     */
    public Path append(Path path) {
        if (path.segments.length == 0) {
            return this;
        }
        String[] concat = new String[segments.length + path.segments.length];
        System.arraycopy(segments, 0, concat, 0, segments.length);
        System.arraycopy(path.segments, 0, concat, segments.length, path.segments.length);
        return new Path(concat, absolute);
    }

    /**
     * 获取fullPath
     */
    public String getPath() {
        if (fullPath == null) {
            fullPath = createFullPath();
        }
        return fullPath;
    }

    private String createFullPath() {
        StringBuilder path = new StringBuilder();
        if (absolute) {
            path.append(SEPARATOR);
        }
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                path.append(SEPARATOR);
            }
            String segment = segments[i];
            path.append(segment);
        }
        return path.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Path path;
        path  = (Path) obj;

        if (absolute != path.absolute) {
            return false;
        }
        return Arrays.equals(segments, path.segments);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(segments);
        result = 31 * result + (absolute ? 1 : 0);
        return result;
    }

    /**
     * Returns the parent of this path, or null if this path has no parent.
     *
     * @return The parent of this path.
     */
    public Path getParent() {
        if (segments.length == 0) {
            return null;
        }
        if (segments.length == 1) {
            return absolute ? ROOT : null;
        }
        String[] parentPath = new String[segments.length - 1];
        System.arraycopy(segments, 0, parentPath, 0, parentPath.length);
        return new Path(parentPath, absolute);
    }

    /**
     * Returns the base name of this path, or null if this path is the root path.
     *
     * @return The base name,
     */
    public String getName() {
        if (segments.length == 0) {
            return null;
        }
        return segments[segments.length - 1];
    }

    /**
     * Creates a child of this path with the given name.
     */
    public Path child(String name) {
        String[] childSegments = new String[segments.length + 1];
        System.arraycopy(segments, 0, childSegments, 0, segments.length);
        childSegments[segments.length] = name;
        return new Path(childSegments, absolute);
    }

    /**
     * Resolves the given name relative to this path. If an absolute path is provided, it is returned.
     */
    public String absolutePath(String path) {
        return absolutePath(path(path)).getPath();
    }

    /**
     * absolute Path
     */
    public Path absolutePath(Path path) {
        if (path.absolute) {
            return path;
        }
        return append(path);
    }

    /**
     * relativePath
     */
    public String relativePath(String path) {
        return relativePath(path(path)).getPath();
    }

    /**
     * relativePath
     */
    public Path relativePath(Path path) {
        if (path.absolute != absolute) {
            return path;
        }
        if (path.segments.length < segments.length) {
            return path;
        }
        for (int i = 0; i < segments.length; i++) {
            if (!path.segments[i].equals(segments[i])) {
                return path;
            }
        }
        if (path.segments.length == segments.length) {
            return path;
        }
        String[] newSegments = Arrays.copyOfRange(path.segments, segments.length, path.segments.length);
        return new Path(newSegments, false);
    }
}
