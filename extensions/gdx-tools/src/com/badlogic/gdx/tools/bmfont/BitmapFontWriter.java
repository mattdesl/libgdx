package com.badlogic.gdx.tools.bmfont;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.utils.Array;

/**
 * A utility to output BitmapFontData to a FNT file. This can be useful for caching the result from
 * TrueTypeFont, for faster load times. 
 * 
 * @author mattdesl AKA davedes
 */
public class BitmapFontWriter {

	/** The Padding parameter for FontInfo. */
	static class Padding {
		public int up, down, left, right;
	}

	/** The spacing parameter for FontInfo. */
	static class Spacing {
		public int horizontal, vertical;
	}

	/** The font "info" line; this will be ignored by LibGDX's BitmapFont reader,
	 * but useful for clean and organized output. */
	static class FontInfo {
		/** Face name */
		public String face;
		/** Font size (pt) */
		public int size = 12;
		/** Whether the font is bold */
		public boolean bold;
		/** Whether the font is italic */
		public boolean italic;
		/** The charset; or null/empty for default */
		public String charset;
		/** Whether the font uses unicode glyphs */
		public boolean unicode = true;
		/** Stretch for height; default to 100% */
		public int stretchH = 100;
		/** Whether smoothing is applied */
		public boolean smooth = true;
		/** Amount of anti-aliasing that was applied to the font */
		public int aa = 2;
		/** Padding that was applied to the font */
		public Padding padding = new Padding();
		/** Horizontal/vertical spacing that was applied to font */
		public Spacing spacing = new Spacing();
		public int outline = 0;
		
		public FontInfo() {
		}
		
		public FontInfo(String face, int size) {
			this.face = face;
			this.size = size;
		}
	}
	
	
	/**
	 * Writes the given BitmapFontData to a file, using the specified <tt>pageRefs</tt> strings as the 
	 * image paths for each texture page. The glyphs in BitmapFontData have a "page" id, which references
	 * the index of the pageRef you specify here. 
	 * 
	 * The FontInfo parameter is useful for cleaner output; such as including a size and font face name hint. 
	 * However, it can be null to use default values. Ultimately, LibGDX ignores the "info" line when reading back
	 * fonts.
	 * 
	 * Likewise, the scaleW and scaleH are only for cleaner output. They are currently ignored by LibGDX's reader.
	 * For maximum compatibility with other BMFont tools, you should use the width and height of your texture pages 
	 * (each page should be the same size).
	 * 
	 * @param fontData the bitmap font
	 * @param pageRefs the references to each texture page image file, generally in the same folder as outFntFile
	 * @param outFntFile the font file to save to (typically ends with '.fnt')
	 * @param info the optional info for the file header; can be null
	 * @param scaleW the width of your texture pages
	 * @param scaleH the height of your texture pages
	 */
	public static void writeFont (BitmapFontData fontData, String[] pageRefs, FileHandle outFntFile, FontInfo info, int scaleW, int scaleH) {
		if (info==null) {
			info = new FontInfo();
			info.face = outFntFile.nameWithoutExtension();
		}
		
		int lineHeight = (int)fontData.lineHeight;
		int pages = pageRefs.length;
		int packed = 0;
		int base = (int)((fontData.capHeight) + (fontData.flipped ? -fontData.ascent : fontData.ascent));
		
		StringBuilder buf = new StringBuilder();
		//INFO LINE
		buf.append("info face=\"")
			.append(info.face==null ? "" : info.face.replaceAll("\"", "'"))
			.append("\" size=").append(info.size)
			.append(" bold=").append(info.bold ? 1 : 0)
			.append(" italic=").append(info.italic ? 1 : 0)
			.append(" charset=\"").append(info.charset==null ? "" : info.charset)
			.append("\" unicode=").append(info.unicode ? 1 : 0)
			.append(" stretchH=").append(info.stretchH)
			.append(" smooth=").append(info.smooth ? 1 : 0)
			.append(" aa=").append(info.aa)
			.append(" padding=")
				.append(info.padding.up).append(",")
				.append(info.padding.down).append(",")
				.append(info.padding.left).append(",")
				.append(info.padding.right)
			.append(" spacing=")
				.append(info.spacing.horizontal).append(",")
				.append(info.spacing.vertical)
			.append("\n");
		
		//COMMON line
		buf.append("common lineHeight=")
			.append(lineHeight)
			.append(" base=").append(base)
			.append(" scaleW=").append(scaleW)
			.append(" scaleH=").append(scaleH)
			.append(" pages=").append(pages)
			.append(" packed=").append(packed)
			.append(" alphaChnl=0 redChnl=0 greenChnl=0 blueChnl=0")
			.append("\n");
		
		//PAGES
		for (int i=0; i<pageRefs.length; i++) {
			buf.append("page id=")
				.append(i)
				.append(" file=\"")
				.append(pageRefs[i])
				.append("\"\n");
		}
		
		//CHARS
		Array<Glyph> glyphs = new Array<Glyph>(256);
		for (int i=0; i<fontData.glyphs.length; i++) {
			if (fontData.glyphs[i]==null)
				continue;
			
			for (int j=0; j<fontData.glyphs[i].length; j++) {
				if (fontData.glyphs[i][j]!=null) {
					glyphs.add(fontData.glyphs[i][j]);
				}
			}
		}
		
		buf.append("chars count=").append(glyphs.size).append("\n");
		
		//CHAR definitions
		for (int i=0; i<glyphs.size; i++) {
			Glyph g = glyphs.get(i);
			buf.append("char id=")
				.append(String.format("%-5s", g.id))
				.append("x=").append(String.format("%-5s", g.srcX))
				.append("y=").append(String.format("%-5s", g.srcY))
				.append("width=").append(String.format("%-5s", g.width))
				.append("height=").append(String.format("%-5s", g.height))
				.append("xoffset=").append(String.format("%-5s", g.xoffset))
				.append("yoffset=").append(String.format("%-5s", fontData.flipped ? g.yoffset : -(g.height + g.yoffset) ))
				.append("xadvance=").append(String.format("%-5s", g.xadvance))
				.append("page=").append(String.format("%-5s", g.page))
				.append("chnl=0")
				.append("\n");
		}
		
		//KERNINGS
		int kernCount = 0;
		StringBuilder kernBuf = new StringBuilder(); 
		for (int i = 0; i < glyphs.size; i++) {
			for (int j = 0; j < glyphs.size; j++) {
				Glyph first = glyphs.get(i);
				Glyph second = glyphs.get(j);
				int kern = first.getKerning((char)second.id);
				if (kern!=0) {
					kernCount++;
					kernBuf.append("kerning first=").append(first.id)
							 .append(" second=").append(second.id)
							 .append(" amount=").append(kern)
							 .append("\n");
				}
			}
		}

		//KERN info
		buf.append("kernings count=").append(kernCount).append("\n");
		buf.append(kernBuf);
		
		String charset = info.charset;
		if (charset!=null&&charset.length()==0)
			charset = null;
		
		outFntFile.writeString(buf.toString(), false, charset);
	}

	
	/**
	 * A utility method which writes the given font data to a file. 
	 * 
	 * The specified pixmaps are written to the parent directory of <tt>outFntFile</tt>, using that file's
	 * name without an extension for the PNG file name(s). 
	 * 
	 * The specified FontInfo is optional, and can be null. 
	 * 
	 *  Typical usage looks like this:
	 *  <pre>
	 *      BitmapFontWriter.writeFont( myFontData, myFontPixmaps, Gdx.files.external("fonts/output.fnt"), new FontInfo("Arial", 16) ); 
	 *  </pre>
	 * 
	 * @param fontData the font data
	 * @param pages the pixmaps to write as PNGs
	 * @param outFntFile the output file for the font definition
	 * @param info the optional font info for the header file, can be null
	 */
	public static void writeFont (BitmapFontData fontData, Pixmap[] pages, FileHandle outFntFile, FontInfo info) {
		String[] pageRefs = writePixmaps(pages, outFntFile.parent(), outFntFile.nameWithoutExtension());
		
		//write the font data
		writeFont(fontData, pageRefs, outFntFile, info, pages[0].getWidth(), pages[0].getHeight());
	}
	
	/**
	 * A utility method to write the given array of pixmaps to the given output directory, with the specified
	 * file name. If the pages array is of length 1, then the resulting file ref will look like: "fileName.png".
	 * 
	 * If the pages array is greater than length 1, the resulting file refs will be appended with "_N", such as
	 * "fileName_0.png", "fileName_1.png", "fileName_2.png" etc.
	 * 
	 * The returned string array can then be passed to the <tt>writeFont</tt> method.
	 * 
	 * Note: None of the pixmaps will be disposed.
	 * 
	 * @param pages the pages of pixmap data to write
	 * @param outputDir the output directory 
	 * @param fileName the file names for the output images
	 * @return the array of string references to be used with <tt>writeFont</tt>
	 */
	public static String[] writePixmaps (Pixmap[] pages, FileHandle outputDir, String fileName) {
		if (pages==null || pages.length==0)
			throw new IllegalArgumentException("no pixmaps supplied to BitmapFontWriter.write");
		
		String[] pageRefs = new String[pages.length];
		
		for (int i=0; i<pages.length; i++) {
			String ref = pages.length==1 ? (fileName+".png") : (fileName+"_"+i+".png");
			
			//the ref for this image
			pageRefs[i] = ref;
						
			//write the PNG in that directory
			PixmapIO.writePNG(outputDir.child(ref), pages[i]);
		}
		return pageRefs;
	}
}
