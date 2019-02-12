package com.github.manevolent.ts3j.api;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 Bert De Geyter
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes a permission that has been assigned to a client,
 * a channel group or a server group.
 * <p>
 * For a complete description of the TS3 permission system, refer to
 * <a href="http://forum.teamspeak.com/threads/49581-The-New-Permission-Documentataions">
 * this post</a> on the TeamSpeak forums.
 * </p>
 */
public class Permission extends Wrapper {

	public Permission(Map<String, String> map) {
		super(map);
	}

    public Permission(String id, int value, boolean negated) {
        super(createPermissionMap(id, value, negated));
    }

    public Permission(String id, int value) {
        this(id, value, false);
    }

	/**
	 * Gets the name of this permission.
	 * <p>
	 * Boolean permissions are prefixed with {@code b_}<br>
	 * Integer permissions are prefixed with {@code i_}
	 * </p>
	 *
	 * @return this permission's name
	 */
	public String getName() {
		return get("permsid");
	}

	/**
	 * Gets the value of this permission assignment.
	 * <p>
	 * Please note that this value doesn't necessarily have to be
	 * the effective permission value for a client, as this assignment
	 * can be overridden by another assignment.
	 * </p><p>
	 * Integer permissions usually have values between 0 and 100,
	 * but any integer value is theoretically valid.
	 * </p><p>
	 * Boolean permissions have a value of {@code 0} to represent
	 * {@code false} and {@code 1} to represent {@code true}.
	 * </p>
	 *
	 * @return the value of this permission assignment
	 */
	public int getValue() {
		return getInt("permvalue");
	}

	/**
	 * Returns {@code true} if this permission is negated.
	 * <p>
	 * Negated means that instead of the highest value, the lowest
	 * value will be selected for this permission instead.
	 * </p>
	 *
	 * @return whether this permission is negated or not
	 */
	public boolean isNegated() {
		return getBoolean("permnegated");
	}

	/**
	 * Returns {@code true} if this permission is skipped.
	 * <p>
	 * Skipped only exists for server group and client permissions,
	 * therefore this value will always be false for channel group permissions.
	 * </p><p>
	 * If a client permission is skipped, it won't be overridden by channel
	 * group permissions.<br>
	 * If a server group permission is skipped, it won't be overridden by
	 * channel group or client permissions.
	 * </p>
	 *
	 * @return whether this permission is negated or not
	 */
	public boolean isSkipped() {
		return getBoolean("permskip");
	}

	private static Map<String, String> createPermissionMap(String id, int value, boolean negated) {
	    Map<String, String> map = new LinkedHashMap<>();
	    map.put("permsid", id);
	    map.put("permvalue", Integer.toString(value));
        map.put("permnegated", Integer.toString(negated ? 1 : 0));
	    return Collections.unmodifiableMap(map);
    }
}
