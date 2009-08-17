// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.entities;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.*;

/**
 * @author Esko Luontola
 * @since 13.11.2008
 */
@Retention(RetentionPolicy.RUNTIME)
@BindingAnnotation
public @interface MaxEntityId {
}
