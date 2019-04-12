/*
 * Copyright 2019-Present Entando Inc. (http://www.entando.com) All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.plugin.avatar.web.rest;

import org.entando.plugin.avatar.config.AvatarStyle;

public class AvatarConfigRequest {

    private AvatarStyle avatarStyle;

    public AvatarStyle getAvatarStyle() {
        return avatarStyle;
    }

    public void setAvatarStyle(AvatarStyle avatarStyle) {
        this.avatarStyle = avatarStyle;
    }
}
