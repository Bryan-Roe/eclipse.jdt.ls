/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Originally copied from org.eclipse.jdt.internal.ui.text.BufferedDocumentScanner
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.partition;


import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;



/**
 * A buffered document scanner. The buffer always contains a section
 * of a fixed size of the document to be scanned.
 */

public final class BufferedDocumentScanner implements ICharacterScanner {

	/** The document being scanned. */
	private IDocument fDocument;
	/** The offset of the document range to scan. */
	private int fRangeOffset;
	/** The length of the document range to scan. */
	private int fRangeLength;
	/** The delimiters of the document. */
	private char[][] fDelimiters;

	/** The buffer. */
	private final char[] fBuffer;
	/** The offset of the buffer within the document. */
	private int fBufferOffset;
	/** The valid length of the buffer for access. */
	private int fBufferLength;
	/** The offset of the scanner within the buffer. */
	private int fOffset;


	/**
	 * Creates a new buffered document scanner.
	 * The buffer size is set to the given number of characters.
	 *
	 * @param size the buffer size
	 */
	public BufferedDocumentScanner(int size) {
		Assert.isTrue(size >= 1);
		fBuffer= new char[size];
	}

	/**
	 * Fills the buffer with the contents of the document starting at the given offset.
	 *
	 * @param offset the document offset at which the buffer starts
	 */
	private void updateBuffer(int offset) {

		fBufferOffset= offset;

		if (fBufferOffset + fBuffer.length > fRangeOffset + fRangeLength) {
			fBufferLength= fRangeLength - (fBufferOffset - fRangeOffset);
		} else {
			fBufferLength= fBuffer.length;
		}

		try {
			final String content= fDocument.get(fBufferOffset, fBufferLength);
			content.getChars(0, fBufferLength, fBuffer, 0);
		} catch (BadLocationException e) {
		}
	}

	/**
	 * Configures the scanner by providing access to the document range over which to scan.
	 *
	 * @param document the document to scan
	 * @param offset the offset of the document range to scan
	 * @param length the length of the document range to scan
	 */
	public void setRange(IDocument document, int offset, int length) {

		fDocument= document;
		fRangeOffset= offset;
		fRangeLength= length;

		String[] delimiters= document.getLegalLineDelimiters();
		fDelimiters= new char[delimiters.length][];
		for (int i= 0; i < delimiters.length; i++) {
			fDelimiters[i]= delimiters[i].toCharArray();
		}

		updateBuffer(offset);
		fOffset= 0;
	}

	/*
	 * @see ICharacterScanner#read()
	 */
	@Override
	public int read() {

		if (fOffset == fBufferLength) {
			int end= fBufferOffset + fBufferLength;
			if (end == fDocument.getLength() || end == fRangeOffset + fRangeLength) {
				return EOF;
			} else {
				updateBuffer(fBufferOffset + fBufferLength);
				fOffset= 0;
			}
		}

		try {
			return fBuffer[fOffset++];
		} catch (ArrayIndexOutOfBoundsException ex) {
			StringBuilder buf= new StringBuilder();
			buf.append("Detailed state of 'BufferedDocumentScanner:'"); //$NON-NLS-1$
			buf.append("\n\tfOffset= "); //$NON-NLS-1$
			buf.append(fOffset);
			buf.append("\n\tfBufferOffset= "); //$NON-NLS-1$
			buf.append(fBufferOffset);
			buf.append("\n\tfBufferLength= "); //$NON-NLS-1$
			buf.append(fBufferLength);
			buf.append("\n\tfRangeOffset= "); //$NON-NLS-1$
			buf.append(fRangeOffset);
			buf.append("\n\tfRangeLength= "); //$NON-NLS-1$
			buf.append(fRangeLength);
			JavaLanguageServerPlugin.logInfo(buf.toString());
			throw ex;
		}
	}

	/*
	 * @see ICharacterScanner#unread
	 */
	@Override
	public void unread() {

		if (fOffset == 0) {
			if (fBufferOffset == fRangeOffset) {
				// error: BOF
			} else {
				updateBuffer(fBufferOffset - fBuffer.length);
				fOffset= fBuffer.length - 1;
			}
		} else {
			--fOffset;
		}
	}

	/*
	 * @see ICharacterScanner#getColumn()
	 */
	@Override
	public int getColumn() {

		try {
			final int offset= fBufferOffset + fOffset;
			final int line= fDocument.getLineOfOffset(offset);
			final int start= fDocument.getLineOffset(line);
			return offset - start;
		} catch (BadLocationException e) {
		}

		return -1;
	}

	/*
	 * @see ICharacterScanner#getLegalLineDelimiters()
	 */
	@Override
	public char[][] getLegalLineDelimiters() {
		return fDelimiters;
	}
}
