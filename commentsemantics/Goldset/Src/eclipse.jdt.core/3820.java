/*******************************************************************************
 * Copyright (c) 2007 BEA Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    wharley@bea.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.apt.tests.annotations.apitest;

/**
 * Triggers the APIAnnotationProcessor, which will validate that the annotated object
 * is of a type that is a subtype of the specified value.
 */
public @interface SubtypeOf {

    Class<?> value();
}
