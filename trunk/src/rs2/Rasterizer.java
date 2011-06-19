package rs2;

public class Rasterizer extends Graphics2D {

	public static void clearCache() {
		anIntArray1468 = null;
		anIntArray1468 = null;
		sineTable = null;
		cosineTable = null;
		lineOffsets = null;
		textureImages = null;
		textureIsTransparent = null;
		averageTextureColour = null;
		texelArrayPool = null;
		texelCache = null;
		textureLastUsed = null;
		hsl2rgb = null;
		texturePalettes = null;
	}

	public static void setDefaultBounds() {
		lineOffsets = new int[Rasterizer.height];
		for (int i = 0; i < height; i++) {
			lineOffsets[i] = width * i;
		}
		centerX = width / 2;
		centerY = height / 2;
	}

	public static void setBounds(int width, int height) {
		lineOffsets = new int[height];
		for (int i = 0; i < height; i++) {
			lineOffsets[i] = width * i;
		}
		centerX = width / 2;
		centerY = height / 2;
	}

	public static void clearTextureCache() {
		texelArrayPool = null;
		for (int i = 0; i < 50; i++) {
			texelCache[i] = null;
		}

	}

	public static void resetTextures(int texturePoolSize) {
		if (texelArrayPool == null) {
			textureTexelPoolPointer = texturePoolSize;//was parameter
			if (lowMem) {
				texelArrayPool = new int[textureTexelPoolPointer][16384];
			} else {
				texelArrayPool = new int[textureTexelPoolPointer][0x10000];
			}
			for (int k = 0; k < 50; k++) {
				texelCache[k] = null;
			}

		}
	}

	public static void unpackTextures(JagexArchive jagexArchive) {
		loadedTextureCount = 0;
		for (int i = 0; i < 50; i++) {
			try {
				textureImages[i] = new IndexedImage(jagexArchive, String.valueOf(i), 0);
				if (lowMem && textureImages[i].libWidth == 128) {
					textureImages[i].resizeToHalfLibSize();
				} else {
					textureImages[i].resizeToLibSize();
				}
				loadedTextureCount++;
			} catch (Exception exception) {
			}
		}

	}

	public static int getAverageTextureColour(int textureId) {
		if (averageTextureColour[textureId] != 0) {
			return averageTextureColour[textureId];
		}
		int red = 0;
		int green = 0;
		int blue = 0;
		int colourCount = texturePalettes[textureId].length;
		for (int ptr = 0; ptr < colourCount; ptr++) {
			red += texturePalettes[textureId][ptr] >> 16 & 0xff;
			green += texturePalettes[textureId][ptr] >> 8 & 0xff;
			blue += texturePalettes[textureId][ptr] & 0xff;
		}

		int rgb = (red / colourCount << 16) + (green / colourCount << 8) + blue / colourCount;
		rgb = adjustBrightness(rgb, 1.3999999999999999D);
		if (rgb == 0) {
			rgb = 1;
		}
		averageTextureColour[textureId] = rgb;
		return rgb;
	}

	public static void resetTexture(int textureId) {
		if (texelCache[textureId] == null) {
			return;
		}
		texelArrayPool[textureTexelPoolPointer++] = texelCache[textureId];
		texelCache[textureId] = null;
	}

	private static int[] getTexturePixels(int textureId) {
		textureLastUsed[textureId] = textureGetCount++;
		if (texelCache[textureId] != null) {
			return texelCache[textureId];
		}
		int texels[];
		//Start of mem management code
		if (textureTexelPoolPointer > 0) {    //Freed texture data arrays available
			texels = texelArrayPool[--textureTexelPoolPointer];
			texelArrayPool[textureTexelPoolPointer] = null;
		} else {   //No freed texture data arrays available, recycle least used texture's array
			int lastUsed = 0;
			int target = -1;
			for (int i = 0; i < loadedTextureCount; i++) {
				if (texelCache[i] != null && (textureLastUsed[i] < lastUsed || target == -1)) {
					lastUsed = textureLastUsed[i];
					target = i;
				}
			}

			texels = texelCache[target];
			texelCache[target] = null;
		}
		texelCache[textureId] = texels;
		//End of mem management code
		IndexedImage indexedImage = textureImages[textureId];
		int texturePalette[] = texturePalettes[textureId];
		if (lowMem) {
			textureIsTransparent[textureId] = false;
			for (int texelPtr = 0; texelPtr < 4096; texelPtr++) {
				int texel = texels[texelPtr] = texturePalette[indexedImage.imgPixels[texelPtr]] & 0xf8f8ff;
				if (texel == 0) {
					textureIsTransparent[textureId] = true;
				}
				texels[4096 + texelPtr] = texel - (texel >>> 3) & 0xf8f8ff;
				texels[8192 + texelPtr] = texel - (texel >>> 2) & 0xf8f8ff;
				texels[12288 + texelPtr] = texel - (texel >>> 2) - (texel >>> 3) & 0xf8f8ff;
			}

		} else {
			if (indexedImage.imgWidth == 64) {
				for (int y = 0; y < 128; y++) {
					for (int x = 0; x < 128; x++) {
						texels[x + (y << 7)] = texturePalette[indexedImage.imgPixels[(x >> 1) + ((y >> 1) << 6)]];
					}

				}

			} else {
				for (int texelPtr = 0; texelPtr < 16384; texelPtr++) {
					texels[texelPtr] = texturePalette[indexedImage.imgPixels[texelPtr]];
				}

			}
			textureIsTransparent[textureId] = false;
			for (int texelPtr = 0; texelPtr < 16384; texelPtr++) {
				texels[texelPtr] &= 0xf8f8ff;
				int texel = texels[texelPtr];
				if (texel == 0) {
					textureIsTransparent[textureId] = true;
				}
				texels[16384 + texelPtr] = texel - (texel >>> 3) & 0xf8f8ff;
				texels[32768 + texelPtr] = texel - (texel >>> 2) & 0xf8f8ff;
				texels[49152 + texelPtr] = texel - (texel >>> 2) - (texel >>> 3) & 0xf8f8ff;
			}

		}
		return texels;
	}

	public static void calculatePalette(double brightness) {
		brightness += Math.random() * 0.029999999999999999D - 0.014999999999999999D;
		int hsl = 0;
		for (int k = 0; k < 512; k++) {
			double d1 = (double) (k / 8) / 64D + 0.0078125D;
			double d2 = (double) (k & 7) / 8D + 0.0625D;
			for (int k1 = 0; k1 < 128; k1++) {
				double d3 = (double) k1 / 128D;
				double r = d3;
				double g = d3;
				double b = d3;
				if (d2 != 0.0D) {
					double d7;
					if (d3 < 0.5D) {
						d7 = d3 * (1.0D + d2);
					} else {
						d7 = (d3 + d2) - d3 * d2;
					}
					double d8 = 2D * d3 - d7;
					double d9 = d1 + 0.33333333333333331D;
					if (d9 > 1.0D) {
						d9--;
					}
					double d10 = d1;
					double d11 = d1 - 0.33333333333333331D;
					if (d11 < 0.0D) {
						d11++;
					}
					if (6D * d9 < 1.0D) {
						r = d8 + (d7 - d8) * 6D * d9;
					} else if (2D * d9 < 1.0D) {
						r = d7;
					} else if (3D * d9 < 2D) {
						r = d8 + (d7 - d8) * (0.66666666666666663D - d9) * 6D;
					} else {
						r = d8;
					}
					if (6D * d10 < 1.0D) {
						g = d8 + (d7 - d8) * 6D * d10;
					} else if (2D * d10 < 1.0D) {
						g = d7;
					} else if (3D * d10 < 2D) {
						g = d8 + (d7 - d8) * (0.66666666666666663D - d10) * 6D;
					} else {
						g = d8;
					}
					if (6D * d11 < 1.0D) {
						b = d8 + (d7 - d8) * 6D * d11;
					} else if (2D * d11 < 1.0D) {
						b = d7;
					} else if (3D * d11 < 2D) {
						b = d8 + (d7 - d8) * (0.66666666666666663D - d11) * 6D;
					} else {
						b = d8;
					}
				}
				int byteR = (int) (r * 256D);
				int byteG = (int) (g * 256D);
				int byteB = (int) (b * 256D);
				int rgb = (byteR << 16) + (byteG << 8) + byteB;
				rgb = adjustBrightness(rgb, brightness);
				if (rgb == 0) {
					rgb = 1;
				}
				hsl2rgb[hsl++] = rgb;
			}

		}

		for (int textureId = 0; textureId < 50; textureId++) {
			if (textureImages[textureId] != null) {
				int palette[] = textureImages[textureId].palette;
				texturePalettes[textureId] = new int[palette.length];
				for (int colourIdx = 0; colourIdx < palette.length; colourIdx++) {
					texturePalettes[textureId][colourIdx] = adjustBrightness(palette[colourIdx], brightness);
					if ((texturePalettes[textureId][colourIdx] & 0xf8f8ff) == 0 && colourIdx != 0) {
						texturePalettes[textureId][colourIdx] = 1;
					}
				}

			}
		}

		for (int textureId = 0; textureId < 50; textureId++) {
			resetTexture(textureId);
		}

	}

	private static int adjustBrightness(int rgb, double intensity) {
		double r = (double) (rgb >> 16) / 256D;
		double g = (double) (rgb >> 8 & 0xff) / 256D;
		double b = (double) (rgb & 0xff) / 256D;
		r = Math.pow(r, intensity);
		g = Math.pow(g, intensity);
		b = Math.pow(b, intensity);
		int r_byte = (int) (r * 256D);
		int g_byte = (int) (g * 256D);
		int b_byte = (int) (b * 256D);
		return (r_byte << 16) + (g_byte << 8) + b_byte;
	}

	public static void drawShadedTriangle(int y_a, int y_b, int y_c, int x_a, int x_b, int x_c, int z_a, int z_b, int z_c) {
		int x_a_off = 0;
		int z_a_off = 0;
		if (y_b != y_a) {
			x_a_off = (x_b - x_a << 16) / (y_b - y_a);
			z_a_off = (z_b - z_a << 15) / (y_b - y_a);
		}
		int x_b_off = 0;
		int z_b_off = 0;
		if (y_c != y_b) {
			x_b_off = (x_c - x_b << 16) / (y_c - y_b);
			z_b_off = (z_c - z_b << 15) / (y_c - y_b);
		}
		int x_c_off = 0;
		int z_c_off = 0;
		if (y_c != y_a) {
			x_c_off = (x_a - x_c << 16) / (y_a - y_c);
			z_c_off = (z_a - z_c << 15) / (y_a - y_c);
		}
		if (y_a <= y_b && y_a <= y_c) {
			if (y_a >= viewport_h) {
				return;
			}
			if (y_b > viewport_h) {
				y_b = viewport_h;
			}
			if (y_c > viewport_h) {
				y_c = viewport_h;
			}
			if (y_b < y_c) {
				x_c = x_a <<= 16;
				z_c = z_a <<= 15;
				if (y_a < 0) {
					x_c -= x_c_off * y_a;
					x_a -= x_a_off * y_a;
					z_c -= z_c_off * y_a;
					z_a -= z_a_off * y_a;
					y_a = 0;
				}
				x_b <<= 16;
				z_b <<= 15;
				if (y_b < 0) {
					x_b -= x_b_off * y_b;
					z_b -= z_b_off * y_b;
					y_b = 0;
				}
				if (y_a != y_b && x_c_off < x_a_off || y_a == y_b && x_c_off > x_b_off) {
					y_c -= y_b;
					y_b -= y_a;
					for (y_a = lineOffsets[y_a]; --y_b >= 0; y_a += width) {
						drawShadedLine(pixels, y_a, x_c >> 16, x_a >> 16, z_c >> 7, z_a >> 7);
						x_c += x_c_off;
						x_a += x_a_off;
						z_c += z_c_off;
						z_a += z_a_off;
					}

					while (--y_c >= 0) {
						drawShadedLine(pixels, y_a, x_c >> 16, x_b >> 16, z_c >> 7, z_b >> 7);
						x_c += x_c_off;
						x_b += x_b_off;
						z_c += z_c_off;
						z_b += z_b_off;
						y_a += width;
					}
					return;
				}
				y_c -= y_b;
				y_b -= y_a;
				for (y_a = lineOffsets[y_a]; --y_b >= 0; y_a += width) {
					drawShadedLine(pixels, y_a, x_a >> 16, x_c >> 16, z_a >> 7, z_c >> 7);
					x_c += x_c_off;
					x_a += x_a_off;
					z_c += z_c_off;
					z_a += z_a_off;
				}

				while (--y_c >= 0) {
					drawShadedLine(pixels, y_a, x_b >> 16, x_c >> 16, z_b >> 7, z_c >> 7);
					x_c += x_c_off;
					x_b += x_b_off;
					z_c += z_c_off;
					z_b += z_b_off;
					y_a += width;
				}
				return;
			}
			x_b = x_a <<= 16;
			z_b = z_a <<= 15;
			if (y_a < 0) {
				x_b -= x_c_off * y_a;
				x_a -= x_a_off * y_a;
				z_b -= z_c_off * y_a;
				z_a -= z_a_off * y_a;
				y_a = 0;
			}
			x_c <<= 16;
			z_c <<= 15;
			if (y_c < 0) {
				x_c -= x_b_off * y_c;
				z_c -= z_b_off * y_c;
				y_c = 0;
			}
			if (y_a != y_c && x_c_off < x_a_off || y_a == y_c && x_b_off > x_a_off) {
				y_b -= y_c;
				y_c -= y_a;
				for (y_a = lineOffsets[y_a]; --y_c >= 0; y_a += width) {
					drawShadedLine(pixels, y_a, x_b >> 16, x_a >> 16, z_b >> 7, z_a >> 7);
					x_b += x_c_off;
					x_a += x_a_off;
					z_b += z_c_off;
					z_a += z_a_off;
				}

				while (--y_b >= 0) {
					drawShadedLine(pixels, y_a, x_c >> 16, x_a >> 16, z_c >> 7, z_a >> 7);
					x_c += x_b_off;
					x_a += x_a_off;
					z_c += z_b_off;
					z_a += z_a_off;
					y_a += width;
				}
				return;
			}
			y_b -= y_c;
			y_c -= y_a;
			for (y_a = lineOffsets[y_a]; --y_c >= 0; y_a += width) {
				drawShadedLine(pixels, y_a, x_a >> 16, x_b >> 16, z_a >> 7, z_b >> 7);
				x_b += x_c_off;
				x_a += x_a_off;
				z_b += z_c_off;
				z_a += z_a_off;
			}

			while (--y_b >= 0) {
				drawShadedLine(pixels, y_a, x_a >> 16, x_c >> 16, z_a >> 7, z_c >> 7);
				x_c += x_b_off;
				x_a += x_a_off;
				z_c += z_b_off;
				z_a += z_a_off;
				y_a += width;
			}
			return;
		}
		if (y_b <= y_c) {
			if (y_b >= viewport_h) {
				return;
			}
			if (y_c > viewport_h) {
				y_c = viewport_h;
			}
			if (y_a > viewport_h) {
				y_a = viewport_h;
			}
			if (y_c < y_a) {
				x_a = x_b <<= 16;
				z_a = z_b <<= 15;
				if (y_b < 0) {
					x_a -= x_a_off * y_b;
					x_b -= x_b_off * y_b;
					z_a -= z_a_off * y_b;
					z_b -= z_b_off * y_b;
					y_b = 0;
				}
				x_c <<= 16;
				z_c <<= 15;
				if (y_c < 0) {
					x_c -= x_c_off * y_c;
					z_c -= z_c_off * y_c;
					y_c = 0;
				}
				if (y_b != y_c && x_a_off < x_b_off || y_b == y_c && x_a_off > x_c_off) {
					y_a -= y_c;
					y_c -= y_b;
					for (y_b = lineOffsets[y_b]; --y_c >= 0; y_b += width) {
						drawShadedLine(pixels, y_b, x_a >> 16, x_b >> 16, z_a >> 7, z_b >> 7);
						x_a += x_a_off;
						x_b += x_b_off;
						z_a += z_a_off;
						z_b += z_b_off;
					}

					while (--y_a >= 0) {
						drawShadedLine(pixels, y_b, x_a >> 16, x_c >> 16, z_a >> 7, z_c >> 7);
						x_a += x_a_off;
						x_c += x_c_off;
						z_a += z_a_off;
						z_c += z_c_off;
						y_b += width;
					}
					return;
				}
				y_a -= y_c;
				y_c -= y_b;
				for (y_b = lineOffsets[y_b]; --y_c >= 0; y_b += width) {
					drawShadedLine(pixels, y_b, x_b >> 16, x_a >> 16, z_b >> 7, z_a >> 7);
					x_a += x_a_off;
					x_b += x_b_off;
					z_a += z_a_off;
					z_b += z_b_off;
				}

				while (--y_a >= 0) {
					drawShadedLine(pixels, y_b, x_c >> 16, x_a >> 16, z_c >> 7, z_a >> 7);
					x_a += x_a_off;
					x_c += x_c_off;
					z_a += z_a_off;
					z_c += z_c_off;
					y_b += width;
				}
				return;
			}
			x_c = x_b <<= 16;
			z_c = z_b <<= 15;
			if (y_b < 0) {
				x_c -= x_a_off * y_b;
				x_b -= x_b_off * y_b;
				z_c -= z_a_off * y_b;
				z_b -= z_b_off * y_b;
				y_b = 0;
			}
			x_a <<= 16;
			z_a <<= 15;
			if (y_a < 0) {
				x_a -= x_c_off * y_a;
				z_a -= z_c_off * y_a;
				y_a = 0;
			}
			if (x_a_off < x_b_off) {
				y_c -= y_a;
				y_a -= y_b;
				for (y_b = lineOffsets[y_b]; --y_a >= 0; y_b += width) {
					drawShadedLine(pixels, y_b, x_c >> 16, x_b >> 16, z_c >> 7, z_b >> 7);
					x_c += x_a_off;
					x_b += x_b_off;
					z_c += z_a_off;
					z_b += z_b_off;
				}

				while (--y_c >= 0) {
					drawShadedLine(pixels, y_b, x_a >> 16, x_b >> 16, z_a >> 7, z_b >> 7);
					x_a += x_c_off;
					x_b += x_b_off;
					z_a += z_c_off;
					z_b += z_b_off;
					y_b += width;
				}
				return;
			}
			y_c -= y_a;
			y_a -= y_b;
			for (y_b = lineOffsets[y_b]; --y_a >= 0; y_b += width) {
				drawShadedLine(pixels, y_b, x_b >> 16, x_c >> 16, z_b >> 7, z_c >> 7);
				x_c += x_a_off;
				x_b += x_b_off;
				z_c += z_a_off;
				z_b += z_b_off;
			}

			while (--y_c >= 0) {
				drawShadedLine(pixels, y_b, x_b >> 16, x_a >> 16, z_b >> 7, z_a >> 7);
				x_a += x_c_off;
				x_b += x_b_off;
				z_a += z_c_off;
				z_b += z_b_off;
				y_b += width;
			}
			return;
		}
		if (y_c >= viewport_h) {
			return;
		}
		if (y_a > viewport_h) {
			y_a = viewport_h;
		}
		if (y_b > viewport_h) {
			y_b = viewport_h;
		}
		if (y_a < y_b) {
			x_b = x_c <<= 16;
			z_b = z_c <<= 15;
			if (y_c < 0) {
				x_b -= x_b_off * y_c;
				x_c -= x_c_off * y_c;
				z_b -= z_b_off * y_c;
				z_c -= z_c_off * y_c;
				y_c = 0;
			}
			x_a <<= 16;
			z_a <<= 15;
			if (y_a < 0) {
				x_a -= x_a_off * y_a;
				z_a -= z_a_off * y_a;
				y_a = 0;
			}
			if (x_b_off < x_c_off) {
				y_b -= y_a;
				y_a -= y_c;
				for (y_c = lineOffsets[y_c]; --y_a >= 0; y_c += width) {
					drawShadedLine(pixels, y_c, x_b >> 16, x_c >> 16, z_b >> 7, z_c >> 7);
					x_b += x_b_off;
					x_c += x_c_off;
					z_b += z_b_off;
					z_c += z_c_off;
				}

				while (--y_b >= 0) {
					drawShadedLine(pixels, y_c, x_b >> 16, x_a >> 16, z_b >> 7, z_a >> 7);
					x_b += x_b_off;
					x_a += x_a_off;
					z_b += z_b_off;
					z_a += z_a_off;
					y_c += width;
				}
				return;
			}
			y_b -= y_a;
			y_a -= y_c;
			for (y_c = lineOffsets[y_c]; --y_a >= 0; y_c += width) {
				drawShadedLine(pixels, y_c, x_c >> 16, x_b >> 16, z_c >> 7, z_b >> 7);
				x_b += x_b_off;
				x_c += x_c_off;
				z_b += z_b_off;
				z_c += z_c_off;
			}

			while (--y_b >= 0) {
				drawShadedLine(pixels, y_c, x_a >> 16, x_b >> 16, z_a >> 7, z_b >> 7);
				x_b += x_b_off;
				x_a += x_a_off;
				z_b += z_b_off;
				z_a += z_a_off;
				y_c += width;
			}
			return;
		}
		x_a = x_c <<= 16;
		z_a = z_c <<= 15;
		if (y_c < 0) {
			x_a -= x_b_off * y_c;
			x_c -= x_c_off * y_c;
			z_a -= z_b_off * y_c;
			z_c -= z_c_off * y_c;
			y_c = 0;
		}
		x_b <<= 16;
		z_b <<= 15;
		if (y_b < 0) {
			x_b -= x_a_off * y_b;
			z_b -= z_a_off * y_b;
			y_b = 0;
		}
		if (x_b_off < x_c_off) {
			y_a -= y_b;
			y_b -= y_c;
			for (y_c = lineOffsets[y_c]; --y_b >= 0; y_c += width) {
				drawShadedLine(pixels, y_c, x_a >> 16, x_c >> 16, z_a >> 7, z_c >> 7);
				x_a += x_b_off;
				x_c += x_c_off;
				z_a += z_b_off;
				z_c += z_c_off;
			}

			while (--y_a >= 0) {
				drawShadedLine(pixels, y_c, x_b >> 16, x_c >> 16, z_b >> 7, z_c >> 7);
				x_b += x_a_off;
				x_c += x_c_off;
				z_b += z_a_off;
				z_c += z_c_off;
				y_c += width;
			}
			return;
		}
		y_a -= y_b;
		y_b -= y_c;
		for (y_c = lineOffsets[y_c]; --y_b >= 0; y_c += width) {
			drawShadedLine(pixels, y_c, x_c >> 16, x_a >> 16, z_c >> 7, z_a >> 7);
			x_a += x_b_off;
			x_c += x_c_off;
			z_a += z_b_off;
			z_c += z_c_off;
		}

		while (--y_a >= 0) {
			drawShadedLine(pixels, y_c, x_c >> 16, x_b >> 16, z_c >> 7, z_b >> 7);
			x_b += x_a_off;
			x_c += x_c_off;
			z_b += z_a_off;
			z_c += z_c_off;
			y_c += width;
		}
	}

	public static void drawShadedLine(int[] dest, int dest_off, int start_x, int end_x, int color_index, int grad) {
		int color;
		int loops;
		int off = 0;
		if (restrict_edges) {
			if (end_x > Graphics2D.viewportRx) {
				end_x = Graphics2D.viewportRx;
			}
			if (start_x < 0) {
				color_index -= start_x * off;
				start_x = 0;
			}
		}
		if (start_x < end_x) {
			dest_off += start_x;
			color_index += off * start_x;
			if (aBoolean1464) {
				loops = end_x - start_x >> 2;
				if (loops > 0) {
					off = (grad - color_index) * anIntArray1468[loops] >> 15;
				} else {
					off = 0;
				}
				if (alpha == 0) {
					if (loops > 0) {
						do {
							color = hsl2rgb[color_index >> 8];
							color_index += off;
							dest[dest_off++] = color;
							dest[dest_off++] = color;
							dest[dest_off++] = color;
							dest[dest_off++] = color;
						} while (--loops > 0);
					}
					loops = end_x - start_x & 0x3;
					if (loops > 0) {
						color = hsl2rgb[color_index >> 8];
						do {
							dest[dest_off++] = color;
						}
						while (--loops > 0);
					}
				} else {
					int src_alpha = alpha;
					int dest_alpha = 256 - alpha;
					if (loops > 0) {
						do {
							color = hsl2rgb[color_index >> 8];
							color_index += off;
							color = (((color & 0xff00ff) * dest_alpha >> 8 & 0xff00ff) + ((color & 0xff00)
									* dest_alpha >> 8 & 0xff00));
							int i = dest[dest_off];
							dest[dest_off++] = (color
									+ ((i & 0xff00ff) * src_alpha >> 8 & 0xff00ff) + ((i & 0xff00)
									* src_alpha >> 8 & 0xff00));
							i = dest[dest_off];
							dest[dest_off++] = (color
									+ ((i & 0xff00ff) * src_alpha >> 8 & 0xff00ff) + ((i & 0xff00)
									* src_alpha >> 8 & 0xff00));
							i = dest[dest_off];
							dest[dest_off++] = (color
									+ ((i & 0xff00ff) * src_alpha >> 8 & 0xff00ff) + ((i & 0xff00)
									* src_alpha >> 8 & 0xff00));
							i = dest[dest_off];
							dest[dest_off++] = (color
									+ ((i & 0xff00ff) * src_alpha >> 8 & 0xff00ff) + ((i & 0xff00)
									* src_alpha >> 8 & 0xff00));
						} while (--loops > 0);
					}
					loops = end_x - start_x & 0x3;
					if (loops > 0) {
						color = hsl2rgb[color_index >> 8];
						color = (((color & 0xff00ff) * dest_alpha >> 8 & 0xff00ff) + ((color & 0xff00)
								* dest_alpha >> 8 & 0xff00));
						do {
							int i = dest[dest_off];
							dest[dest_off++] = (color
									+ ((i & 0xff00ff) * src_alpha >> 8 & 0xff00ff) + ((i & 0xff00)
									* src_alpha >> 8 & 0xff00));
						} while (--loops > 0);
					}
				}
			} else {
				int col_off = (grad - color_index) / (end_x - start_x);
				loops = end_x - start_x;
				if (alpha == 0) {
					do {
						dest[dest_off++] = hsl2rgb[color_index >> 8];
						color_index += col_off;
					} while (--loops > 0);
				} else {
					int src_alpha = alpha;
					int dest_alpha = 256 - alpha;
					do {
						color = hsl2rgb[color_index >> 8];
						color_index += col_off;
						color = (((color & 0xff00ff) * dest_alpha >> 8 & 0xff00ff) + ((color & 0xff00)
								* dest_alpha >> 8 & 0xff00));
						int i = dest[dest_off];
						dest[dest_off++] = (color
								+ ((i & 0xff00ff) * src_alpha >> 8 & 0xff00ff) + ((i & 0xff00)
								* src_alpha >> 8 & 0xff00));
					} while (--loops > 0);
				}
			}
		}
	}

	// 317 drawShadedLine, causes lines in transparent models.
	/*private static void drawShadedLine(int ai[], int i, int l, int i1, int jgx, int k1) {
			int rgb;//was parameter
			int k;//was parameter
			if (aBoolean1464) {
				int l1;
				if (restrict_edges) {
					if (i1 - l > 3)
						l1 = (k1 - jgx) / (i1 - l);
					else
						l1 = 0;
					if (i1 > viewportRx)
						i1 = viewportRx;
					if (l < 0) {
						jgx -= l * l1;
						l = 0;
					}
					if (l >= i1)
						return;
					i += l;
					k = i1 - l >> 2;
					l1 <<= 2;
				} else {
					if (l >= i1)
						return;
					i += l;
					k = i1 - l >> 2;
					if (k > 0)
						l1 = (k1 - jgx) * anIntArray1468[k] >> 15;
					else
						l1 = 0;
				}
				if (alpha == 0) {
					while (--k >= 0) {
						rgb = hsl2rgb[jgx >> 8];
						jgx += l1;
						ai[i++] = rgb;
						ai[i++] = rgb;
						ai[i++] = rgb;
						ai[i++] = rgb;
					}
					k = i1 - l & 3;
					if (k > 0) {
						rgb = hsl2rgb[jgx >> 8];
						do
							ai[i++] = rgb;
						while (--k > 0);
						return;
					}
				} else {
					int j2 = alpha;
					int l2 = 256 - alpha;
					while (--k >= 0) {
						rgb = hsl2rgb[jgx >> 8];
						jgx += l1;
						rgb = ((rgb & 0xff00ff) * l2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * l2 >> 8 & 0xff00);
						ai[i++] = rgb + ((ai[i] & 0xff00ff) * j2 >> 8 & 0xff00ff) + ((ai[i] & 0xff00) * j2 >> 8 & 0xff00);
						ai[i++] = rgb + ((ai[i] & 0xff00ff) * j2 >> 8 & 0xff00ff) + ((ai[i] & 0xff00) * j2 >> 8 & 0xff00);
						ai[i++] = rgb + ((ai[i] & 0xff00ff) * j2 >> 8 & 0xff00ff) + ((ai[i] & 0xff00) * j2 >> 8 & 0xff00);
						ai[i++] = rgb + ((ai[i] & 0xff00ff) * j2 >> 8 & 0xff00ff) + ((ai[i] & 0xff00) * j2 >> 8 & 0xff00);
					}
					k = i1 - l & 3;
					if (k > 0) {
						rgb = hsl2rgb[jgx >> 8];
						rgb = ((rgb & 0xff00ff) * l2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * l2 >> 8 & 0xff00);
						do
							ai[i++] = rgb + ((ai[i] & 0xff00ff) * j2 >> 8 & 0xff00ff) + ((ai[i] & 0xff00) * j2 >> 8 & 0xff00);
						while (--k > 0);
					}
				}
				return;
			}
			if (l >= i1)
				return;
			int i2 = (k1 - jgx) / (i1 - l);
			if (restrict_edges) {
				if (i1 > viewportRx)
					i1 = viewportRx;
				if (l < 0) {
					jgx -= l * i2;
					l = 0;
				}
				if (l >= i1)
					return;
			}
			i += l;
			k = i1 - l;
			if (alpha == 0) {
				do {
					ai[i++] = hsl2rgb[jgx >> 8];
					jgx += i2;
				} while (--k > 0);
				return;
			}
			int k2 = alpha;
			int i3 = 256 - alpha;
			do {
				rgb = hsl2rgb[jgx >> 8];
				jgx += i2;
				rgb = ((rgb & 0xff00ff) * i3 >> 8 & 0xff00ff) + ((rgb & 0xff00) * i3 >> 8 & 0xff00);
				ai[i++] = rgb + ((ai[i] & 0xff00ff) * k2 >> 8 & 0xff00ff) + ((ai[i] & 0xff00) * k2 >> 8 & 0xff00);
			} while (--k > 0);
		}*/

	public static void drawFlatTriangle(int y_a, int y_b, int y_c, int x_a, int x_b, int x_c, int color) {
		int x_a_off = 0;
		if (y_b != y_a) {
			x_a_off = (x_b - x_a << 16) / (y_b - y_a);
		}
		int x_b_off = 0;
		if (y_c != y_b) {
			x_b_off = (x_c - x_b << 16) / (y_c - y_b);
		}
		int x_c_off = 0;
		if (y_c != y_a) {
			x_c_off = (x_a - x_c << 16) / (y_a - y_c);
		}
		if (y_a <= y_b && y_a <= y_c) {
			if (y_a >= viewport_h) {
				return;
			}
			if (y_b > viewport_h) {
				y_b = viewport_h;
			}
			if (y_c > viewport_h) {
				y_c = viewport_h;
			}
			if (y_b < y_c) {
				x_c = x_a <<= 16;
				if (y_a < 0) {
					x_c -= x_c_off * y_a;
					x_a -= x_a_off * y_a;
					y_a = 0;
				}
				x_b <<= 16;
				if (y_b < 0) {
					x_b -= x_b_off * y_b;
					y_b = 0;
				}
				if (y_a != y_b && x_c_off < x_a_off || y_a == y_b && x_c_off > x_b_off) {
					y_c -= y_b;
					y_b -= y_a;
					for (y_a = lineOffsets[y_a]; --y_b >= 0; y_a += width) {
						drawScanLine(pixels, y_a, color, x_c >> 16, x_a >> 16);
						x_c += x_c_off;
						x_a += x_a_off;
					}

					while (--y_c >= 0) {
						drawScanLine(pixels, y_a, color, x_c >> 16, x_b >> 16);
						x_c += x_c_off;
						x_b += x_b_off;
						y_a += width;
					}
					return;
				}
				y_c -= y_b;
				y_b -= y_a;
				for (y_a = lineOffsets[y_a]; --y_b >= 0; y_a += width) {
					drawScanLine(pixels, y_a, color, x_a >> 16, x_c >> 16);
					x_c += x_c_off;
					x_a += x_a_off;
				}

				while (--y_c >= 0) {
					drawScanLine(pixels, y_a, color, x_b >> 16, x_c >> 16);
					x_c += x_c_off;
					x_b += x_b_off;
					y_a += width;
				}
				return;
			}
			x_b = x_a <<= 16;
			if (y_a < 0) {
				x_b -= x_c_off * y_a;
				x_a -= x_a_off * y_a;
				y_a = 0;
			}
			x_c <<= 16;
			if (y_c < 0) {
				x_c -= x_b_off * y_c;
				y_c = 0;
			}
			if (y_a != y_c && x_c_off < x_a_off || y_a == y_c && x_b_off > x_a_off) {
				y_b -= y_c;
				y_c -= y_a;
				for (y_a = lineOffsets[y_a]; --y_c >= 0; y_a += width) {
					drawScanLine(pixels, y_a, color, x_b >> 16, x_a >> 16);
					x_b += x_c_off;
					x_a += x_a_off;
				}

				while (--y_b >= 0) {
					drawScanLine(pixels, y_a, color, x_c >> 16, x_a >> 16);
					x_c += x_b_off;
					x_a += x_a_off;
					y_a += width;
				}
				return;
			}
			y_b -= y_c;
			y_c -= y_a;
			for (y_a = lineOffsets[y_a]; --y_c >= 0; y_a += width) {
				drawScanLine(pixels, y_a, color, x_a >> 16, x_b >> 16);
				x_b += x_c_off;
				x_a += x_a_off;
			}

			while (--y_b >= 0) {
				drawScanLine(pixels, y_a, color, x_a >> 16, x_c >> 16);
				x_c += x_b_off;
				x_a += x_a_off;
				y_a += width;
			}
			return;
		}
		if (y_b <= y_c) {
			if (y_b >= viewport_h) {
				return;
			}
			if (y_c > viewport_h) {
				y_c = viewport_h;
			}
			if (y_a > viewport_h) {
				y_a = viewport_h;
			}
			if (y_c < y_a) {
				x_a = x_b <<= 16;
				if (y_b < 0) {
					x_a -= x_a_off * y_b;
					x_b -= x_b_off * y_b;
					y_b = 0;
				}
				x_c <<= 16;
				if (y_c < 0) {
					x_c -= x_c_off * y_c;
					y_c = 0;
				}
				if (y_b != y_c && x_a_off < x_b_off || y_b == y_c && x_a_off > x_c_off) {
					y_a -= y_c;
					y_c -= y_b;
					for (y_b = lineOffsets[y_b]; --y_c >= 0; y_b += width) {
						drawScanLine(pixels, y_b, color, x_a >> 16, x_b >> 16);
						x_a += x_a_off;
						x_b += x_b_off;
					}

					while (--y_a >= 0) {
						drawScanLine(pixels, y_b, color, x_a >> 16, x_c >> 16);
						x_a += x_a_off;
						x_c += x_c_off;
						y_b += width;
					}
					return;
				}
				y_a -= y_c;
				y_c -= y_b;
				for (y_b = lineOffsets[y_b]; --y_c >= 0; y_b += width) {
					drawScanLine(pixels, y_b, color, x_b >> 16, x_a >> 16);
					x_a += x_a_off;
					x_b += x_b_off;
				}

				while (--y_a >= 0) {
					drawScanLine(pixels, y_b, color, x_c >> 16, x_a >> 16);
					x_a += x_a_off;
					x_c += x_c_off;
					y_b += width;
				}
				return;
			}
			x_c = x_b <<= 16;
			if (y_b < 0) {
				x_c -= x_a_off * y_b;
				x_b -= x_b_off * y_b;
				y_b = 0;
			}
			x_a <<= 16;
			if (y_a < 0) {
				x_a -= x_c_off * y_a;
				y_a = 0;
			}
			if (x_a_off < x_b_off) {
				y_c -= y_a;
				y_a -= y_b;
				for (y_b = lineOffsets[y_b]; --y_a >= 0; y_b += width) {
					drawScanLine(pixels, y_b, color, x_c >> 16, x_b >> 16);
					x_c += x_a_off;
					x_b += x_b_off;
				}

				while (--y_c >= 0) {
					drawScanLine(pixels, y_b, color, x_a >> 16, x_b >> 16);
					x_a += x_c_off;
					x_b += x_b_off;
					y_b += width;
				}
				return;
			}
			y_c -= y_a;
			y_a -= y_b;
			for (y_b = lineOffsets[y_b]; --y_a >= 0; y_b += width) {
				drawScanLine(pixels, y_b, color, x_b >> 16, x_c >> 16);
				x_c += x_a_off;
				x_b += x_b_off;
			}

			while (--y_c >= 0) {
				drawScanLine(pixels, y_b, color, x_b >> 16, x_a >> 16);
				x_a += x_c_off;
				x_b += x_b_off;
				y_b += width;
			}
			return;
		}
		if (y_c >= viewport_h) {
			return;
		}
		if (y_a > viewport_h) {
			y_a = viewport_h;
		}
		if (y_b > viewport_h) {
			y_b = viewport_h;
		}
		if (y_a < y_b) {
			x_b = x_c <<= 16;
			if (y_c < 0) {
				x_b -= x_b_off * y_c;
				x_c -= x_c_off * y_c;
				y_c = 0;
			}
			x_a <<= 16;
			if (y_a < 0) {
				x_a -= x_a_off * y_a;
				y_a = 0;
			}
			if (x_b_off < x_c_off) {
				y_b -= y_a;
				y_a -= y_c;
				for (y_c = lineOffsets[y_c]; --y_a >= 0; y_c += width) {
					drawScanLine(pixels, y_c, color, x_b >> 16, x_c >> 16);
					x_b += x_b_off;
					x_c += x_c_off;
				}

				while (--y_b >= 0) {
					drawScanLine(pixels, y_c, color, x_b >> 16, x_a >> 16);
					x_b += x_b_off;
					x_a += x_a_off;
					y_c += width;
				}
				return;
			}
			y_b -= y_a;
			y_a -= y_c;
			for (y_c = lineOffsets[y_c]; --y_a >= 0; y_c += width) {
				drawScanLine(pixels, y_c, color, x_c >> 16, x_b >> 16);
				x_b += x_b_off;
				x_c += x_c_off;
			}

			while (--y_b >= 0) {
				drawScanLine(pixels, y_c, color, x_a >> 16, x_b >> 16);
				x_b += x_b_off;
				x_a += x_a_off;
				y_c += width;
			}
			return;
		}
		x_a = x_c <<= 16;
		if (y_c < 0) {
			x_a -= x_b_off * y_c;
			x_c -= x_c_off * y_c;
			y_c = 0;
		}
		x_b <<= 16;
		if (y_b < 0) {
			x_b -= x_a_off * y_b;
			y_b = 0;
		}
		if (x_b_off < x_c_off) {
			y_a -= y_b;
			y_b -= y_c;
			for (y_c = lineOffsets[y_c]; --y_b >= 0; y_c += width) {
				drawScanLine(pixels, y_c, color, x_a >> 16, x_c >> 16);
				x_a += x_b_off;
				x_c += x_c_off;
			}

			while (--y_a >= 0) {
				drawScanLine(pixels, y_c, color, x_b >> 16, x_c >> 16);
				x_b += x_a_off;
				x_c += x_c_off;
				y_c += width;
			}
			return;
		}
		y_a -= y_b;
		y_b -= y_c;
		for (y_c = lineOffsets[y_c]; --y_b >= 0; y_c += width) {
			drawScanLine(pixels, y_c, color, x_c >> 16, x_a >> 16);
			x_a += x_b_off;
			x_c += x_c_off;
		}

		while (--y_a >= 0) {
			drawScanLine(pixels, y_c, color, x_c >> 16, x_b >> 16);
			x_b += x_a_off;
			x_c += x_c_off;
			y_c += width;
		}
	}

	private static void drawScanLine(int dest[], int dest_off, int loops, int start_x, int end_x) {
		int rgb;//was parameter
		if (restrict_edges) {
			if (end_x > viewportRx) {
				end_x = viewportRx;
			}
			if (start_x < 0) {
				start_x = 0;
			}
		}
		if (start_x >= end_x) {
			return;
		}
		dest_off += start_x;
		rgb = end_x - start_x >> 2;
		if (alpha == 0) {
			while (--rgb >= 0) {
				dest[dest_off++] = loops;
				dest[dest_off++] = loops;
				dest[dest_off++] = loops;
				dest[dest_off++] = loops;
			}
			for (rgb = end_x - start_x & 3; --rgb >= 0; ) {
				dest[dest_off++] = loops;
			}

			return;
		}
		int dest_alpha = alpha;
		int src_alpha = 256 - alpha;
		loops = ((loops & 0xff00ff) * src_alpha >> 8 & 0xff00ff) + ((loops & 0xff00) * src_alpha >> 8 & 0xff00);
		while (--rgb >= 0) {
			dest[dest_off++] = loops + ((dest[dest_off] & 0xff00ff) * dest_alpha >> 8 & 0xff00ff) + ((dest[dest_off] & 0xff00) * dest_alpha >> 8 & 0xff00);
			dest[dest_off++] = loops + ((dest[dest_off] & 0xff00ff) * dest_alpha >> 8 & 0xff00ff) + ((dest[dest_off] & 0xff00) * dest_alpha >> 8 & 0xff00);
			dest[dest_off++] = loops + ((dest[dest_off] & 0xff00ff) * dest_alpha >> 8 & 0xff00ff) + ((dest[dest_off] & 0xff00) * dest_alpha >> 8 & 0xff00);
			dest[dest_off++] = loops + ((dest[dest_off] & 0xff00ff) * dest_alpha >> 8 & 0xff00ff) + ((dest[dest_off] & 0xff00) * dest_alpha >> 8 & 0xff00);
		}
		for (rgb = end_x - start_x & 3; --rgb >= 0; ) {
			dest[dest_off++] = loops + ((dest[dest_off] & 0xff00ff) * dest_alpha >> 8 & 0xff00ff) + ((dest[dest_off] & 0xff00) * dest_alpha >> 8 & 0xff00);
		}

	}

	public static void drawTexturedTriangle(int y_a, int y_b, int y_c, int x_a, int x_b, int x_c, int grad_a, int grad_b,
	                                        int grad_c, int t_x_a, int t_x_b, int t_x_c, int t_y_a, int t_y_b, int t_y_c,
	                                        int t_z_a, int t_z_b, int t_z_c, int t_id) {
		int texture[] = getTexturePixels(t_id);
		Rasterizer.opaque = !textureIsTransparent[t_id];
		t_x_b = t_x_a - t_x_b;
		t_y_b = t_y_a - t_y_b;
		t_z_b = t_z_a - t_z_b;
		t_x_c -= t_x_a;
		t_y_c -= t_y_a;
		t_z_c -= t_z_a;
		int l4 = t_x_c * t_y_a - t_y_c * t_x_a << 14;
		int i5 = t_y_c * t_z_a - t_z_c * t_y_a << 8;
		int j5 = t_z_c * t_x_a - t_x_c * t_z_a << 5;
		int k5 = t_x_b * t_y_a - t_y_b * t_x_a << 14;
		int l5 = t_y_b * t_z_a - t_z_b * t_y_a << 8;
		int i6 = t_z_b * t_x_a - t_x_b * t_z_a << 5;
		int j6 = t_y_b * t_x_c - t_x_b * t_y_c << 14;
		int k6 = t_z_b * t_y_c - t_y_b * t_z_c << 8;
		int l6 = t_x_b * t_z_c - t_z_b * t_x_c << 5;
		int x_a_off = 0;
		int grad_a_off = 0;
		if (y_b != y_a) {
			x_a_off = (x_b - x_a << 16) / (y_b - y_a);
			grad_a_off = (grad_b - grad_a << 16) / (y_b - y_a);
		}
		int x_b_off = 0;
		int grad_b_off = 0;
		if (y_c != y_b) {
			x_b_off = (x_c - x_b << 16) / (y_c - y_b);
			grad_b_off = (grad_c - grad_b << 16) / (y_c - y_b);
		}
		int x_c_off = 0;
		int grad_c_off = 0;
		if (y_c != y_a) {
			x_c_off = (x_a - x_c << 16) / (y_a - y_c);
			grad_c_off = (grad_a - grad_c << 16) / (y_a - y_c);
		}
		if (y_a <= y_b && y_a <= y_c) {
			if (y_a >= viewport_h) {
				return;
			}
			if (y_b > viewport_h) {
				y_b = viewport_h;
			}
			if (y_c > viewport_h) {
				y_c = viewport_h;
			}
			if (y_b < y_c) {
				x_c = x_a <<= 16;
				grad_c = grad_a <<= 16;
				if (y_a < 0) {
					x_c -= x_c_off * y_a;
					x_a -= x_a_off * y_a;
					grad_c -= grad_c_off * y_a;
					grad_a -= grad_a_off * y_a;
					y_a = 0;
				}
				x_b <<= 16;
				grad_b <<= 16;
				if (y_b < 0) {
					x_b -= x_b_off * y_b;
					grad_b -= grad_b_off * y_b;
					y_b = 0;
				}
				int k8 = y_a - centerY;
				l4 += j5 * k8;
				k5 += i6 * k8;
				j6 += l6 * k8;
				if (y_a != y_b && x_c_off < x_a_off || y_a == y_b && x_c_off > x_b_off) {
					y_c -= y_b;
					y_b -= y_a;
					y_a = lineOffsets[y_a];
					while (--y_b >= 0) {
						drawTexturedLine(pixels, texture, y_a, x_c >> 16, x_a >> 16, grad_c >> 8, grad_a >> 8, l4, k5, j6, i5, l5, k6);
						x_c += x_c_off;
						x_a += x_a_off;
						grad_c += grad_c_off;
						grad_a += grad_a_off;
						y_a += width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					while (--y_c >= 0) {
						drawTexturedLine(pixels, texture, y_a, x_c >> 16, x_b >> 16, grad_c >> 8, grad_b >> 8, l4, k5, j6, i5, l5, k6);
						x_c += x_c_off;
						x_b += x_b_off;
						grad_c += grad_c_off;
						grad_b += grad_b_off;
						y_a += width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					return;
				}
				y_c -= y_b;
				y_b -= y_a;
				y_a = lineOffsets[y_a];
				while (--y_b >= 0) {
					drawTexturedLine(pixels, texture, y_a, x_a >> 16, x_c >> 16, grad_a >> 8, grad_c >> 8, l4, k5, j6, i5, l5, k6);
					x_c += x_c_off;
					x_a += x_a_off;
					grad_c += grad_c_off;
					grad_a += grad_a_off;
					y_a += width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y_c >= 0) {
					drawTexturedLine(pixels, texture, y_a, x_b >> 16, x_c >> 16, grad_b >> 8, grad_c >> 8, l4, k5, j6, i5, l5, k6);
					x_c += x_c_off;
					x_b += x_b_off;
					grad_c += grad_c_off;
					grad_b += grad_b_off;
					y_a += width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				return;
			}
			x_b = x_a <<= 16;
			grad_b = grad_a <<= 16;
			if (y_a < 0) {
				x_b -= x_c_off * y_a;
				x_a -= x_a_off * y_a;
				grad_b -= grad_c_off * y_a;
				grad_a -= grad_a_off * y_a;
				y_a = 0;
			}
			x_c <<= 16;
			grad_c <<= 16;
			if (y_c < 0) {
				x_c -= x_b_off * y_c;
				grad_c -= grad_b_off * y_c;
				y_c = 0;
			}
			int l8 = y_a - centerY;
			l4 += j5 * l8;
			k5 += i6 * l8;
			j6 += l6 * l8;
			if (y_a != y_c && x_c_off < x_a_off || y_a == y_c && x_b_off > x_a_off) {
				y_b -= y_c;
				y_c -= y_a;
				y_a = lineOffsets[y_a];
				while (--y_c >= 0) {
					drawTexturedLine(pixels, texture, y_a, x_b >> 16, x_a >> 16, grad_b >> 8, grad_a >> 8, l4, k5, j6, i5, l5, k6);
					x_b += x_c_off;
					x_a += x_a_off;
					grad_b += grad_c_off;
					grad_a += grad_a_off;
					y_a += width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y_b >= 0) {
					drawTexturedLine(pixels, texture, y_a, x_c >> 16, x_a >> 16, grad_c >> 8, grad_a >> 8, l4, k5, j6, i5, l5, k6);
					x_c += x_b_off;
					x_a += x_a_off;
					grad_c += grad_b_off;
					grad_a += grad_a_off;
					y_a += width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				return;
			}
			y_b -= y_c;
			y_c -= y_a;
			y_a = lineOffsets[y_a];
			while (--y_c >= 0) {
				drawTexturedLine(pixels, texture, y_a, x_a >> 16, x_b >> 16, grad_a >> 8, grad_b >> 8, l4, k5, j6, i5, l5, k6);
				x_b += x_c_off;
				x_a += x_a_off;
				grad_b += grad_c_off;
				grad_a += grad_a_off;
				y_a += width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y_b >= 0) {
				drawTexturedLine(pixels, texture, y_a, x_a >> 16, x_c >> 16, grad_a >> 8, grad_c >> 8, l4, k5, j6, i5, l5, k6);
				x_c += x_b_off;
				x_a += x_a_off;
				grad_c += grad_b_off;
				grad_a += grad_a_off;
				y_a += width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			return;
		}
		if (y_b <= y_c) {
			if (y_b >= viewport_h) {
				return;
			}
			if (y_c > viewport_h) {
				y_c = viewport_h;
			}
			if (y_a > viewport_h) {
				y_a = viewport_h;
			}
			if (y_c < y_a) {
				x_a = x_b <<= 16;
				grad_a = grad_b <<= 16;
				if (y_b < 0) {
					x_a -= x_a_off * y_b;
					x_b -= x_b_off * y_b;
					grad_a -= grad_a_off * y_b;
					grad_b -= grad_b_off * y_b;
					y_b = 0;
				}
				x_c <<= 16;
				grad_c <<= 16;
				if (y_c < 0) {
					x_c -= x_c_off * y_c;
					grad_c -= grad_c_off * y_c;
					y_c = 0;
				}
				int i9 = y_b - centerY;
				l4 += j5 * i9;
				k5 += i6 * i9;
				j6 += l6 * i9;
				if (y_b != y_c && x_a_off < x_b_off || y_b == y_c && x_a_off > x_c_off) {
					y_a -= y_c;
					y_c -= y_b;
					y_b = lineOffsets[y_b];
					while (--y_c >= 0) {
						drawTexturedLine(pixels, texture, y_b, x_a >> 16, x_b >> 16, grad_a >> 8, grad_b >> 8, l4, k5, j6, i5, l5, k6);
						x_a += x_a_off;
						x_b += x_b_off;
						grad_a += grad_a_off;
						grad_b += grad_b_off;
						y_b += width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					while (--y_a >= 0) {
						drawTexturedLine(pixels, texture, y_b, x_a >> 16, x_c >> 16, grad_a >> 8, grad_c >> 8, l4, k5, j6, i5, l5, k6);
						x_a += x_a_off;
						x_c += x_c_off;
						grad_a += grad_a_off;
						grad_c += grad_c_off;
						y_b += width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					return;
				}
				y_a -= y_c;
				y_c -= y_b;
				y_b = lineOffsets[y_b];
				while (--y_c >= 0) {
					drawTexturedLine(pixels, texture, y_b, x_b >> 16, x_a >> 16, grad_b >> 8, grad_a >> 8, l4, k5, j6, i5, l5, k6);
					x_a += x_a_off;
					x_b += x_b_off;
					grad_a += grad_a_off;
					grad_b += grad_b_off;
					y_b += width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y_a >= 0) {
					drawTexturedLine(pixels, texture, y_b, x_c >> 16, x_a >> 16, grad_c >> 8, grad_a >> 8, l4, k5, j6, i5, l5, k6);
					x_a += x_a_off;
					x_c += x_c_off;
					grad_a += grad_a_off;
					grad_c += grad_c_off;
					y_b += width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				return;
			}
			x_c = x_b <<= 16;
			grad_c = grad_b <<= 16;
			if (y_b < 0) {
				x_c -= x_a_off * y_b;
				x_b -= x_b_off * y_b;
				grad_c -= grad_a_off * y_b;
				grad_b -= grad_b_off * y_b;
				y_b = 0;
			}
			x_a <<= 16;
			grad_a <<= 16;
			if (y_a < 0) {
				x_a -= x_c_off * y_a;
				grad_a -= grad_c_off * y_a;
				y_a = 0;
			}
			int j9 = y_b - centerY;
			l4 += j5 * j9;
			k5 += i6 * j9;
			j6 += l6 * j9;
			if (x_a_off < x_b_off) {
				y_c -= y_a;
				y_a -= y_b;
				y_b = lineOffsets[y_b];
				while (--y_a >= 0) {
					drawTexturedLine(pixels, texture, y_b, x_c >> 16, x_b >> 16, grad_c >> 8, grad_b >> 8, l4, k5, j6, i5, l5, k6);
					x_c += x_a_off;
					x_b += x_b_off;
					grad_c += grad_a_off;
					grad_b += grad_b_off;
					y_b += width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y_c >= 0) {
					drawTexturedLine(pixels, texture, y_b, x_a >> 16, x_b >> 16, grad_a >> 8, grad_b >> 8, l4, k5, j6, i5, l5, k6);
					x_a += x_c_off;
					x_b += x_b_off;
					grad_a += grad_c_off;
					grad_b += grad_b_off;
					y_b += width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				return;
			}
			y_c -= y_a;
			y_a -= y_b;
			y_b = lineOffsets[y_b];
			while (--y_a >= 0) {
				drawTexturedLine(pixels, texture, y_b, x_b >> 16, x_c >> 16, grad_b >> 8, grad_c >> 8, l4, k5, j6, i5, l5, k6);
				x_c += x_a_off;
				x_b += x_b_off;
				grad_c += grad_a_off;
				grad_b += grad_b_off;
				y_b += width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y_c >= 0) {
				drawTexturedLine(pixels, texture, y_b, x_b >> 16, x_a >> 16, grad_b >> 8, grad_a >> 8, l4, k5, j6, i5, l5, k6);
				x_a += x_c_off;
				x_b += x_b_off;
				grad_a += grad_c_off;
				grad_b += grad_b_off;
				y_b += width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			return;
		}
		if (y_c >= viewport_h) {
			return;
		}
		if (y_a > viewport_h) {
			y_a = viewport_h;
		}
		if (y_b > viewport_h) {
			y_b = viewport_h;
		}
		if (y_a < y_b) {
			x_b = x_c <<= 16;
			grad_b = grad_c <<= 16;
			if (y_c < 0) {
				x_b -= x_b_off * y_c;
				x_c -= x_c_off * y_c;
				grad_b -= grad_b_off * y_c;
				grad_c -= grad_c_off * y_c;
				y_c = 0;
			}
			x_a <<= 16;
			grad_a <<= 16;
			if (y_a < 0) {
				x_a -= x_a_off * y_a;
				grad_a -= grad_a_off * y_a;
				y_a = 0;
			}
			int k9 = y_c - centerY;
			l4 += j5 * k9;
			k5 += i6 * k9;
			j6 += l6 * k9;
			if (x_b_off < x_c_off) {
				y_b -= y_a;
				y_a -= y_c;
				y_c = lineOffsets[y_c];
				while (--y_a >= 0) {
					drawTexturedLine(pixels, texture, y_c, x_b >> 16, x_c >> 16, grad_b >> 8, grad_c >> 8, l4, k5, j6, i5, l5, k6);
					x_b += x_b_off;
					x_c += x_c_off;
					grad_b += grad_b_off;
					grad_c += grad_c_off;
					y_c += width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y_b >= 0) {
					drawTexturedLine(pixels, texture, y_c, x_b >> 16, x_a >> 16, grad_b >> 8, grad_a >> 8, l4, k5, j6, i5, l5, k6);
					x_b += x_b_off;
					x_a += x_a_off;
					grad_b += grad_b_off;
					grad_a += grad_a_off;
					y_c += width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				return;
			}
			y_b -= y_a;
			y_a -= y_c;
			y_c = lineOffsets[y_c];
			while (--y_a >= 0) {
				drawTexturedLine(pixels, texture, y_c, x_c >> 16, x_b >> 16, grad_c >> 8, grad_b >> 8, l4, k5, j6, i5, l5, k6);
				x_b += x_b_off;
				x_c += x_c_off;
				grad_b += grad_b_off;
				grad_c += grad_c_off;
				y_c += width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y_b >= 0) {
				drawTexturedLine(pixels, texture, y_c, x_a >> 16, x_b >> 16, grad_a >> 8, grad_b >> 8, l4, k5, j6, i5, l5, k6);
				x_b += x_b_off;
				x_a += x_a_off;
				grad_b += grad_b_off;
				grad_a += grad_a_off;
				y_c += width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			return;
		}
		x_a = x_c <<= 16;
		grad_a = grad_c <<= 16;
		if (y_c < 0) {
			x_a -= x_b_off * y_c;
			x_c -= x_c_off * y_c;
			grad_a -= grad_b_off * y_c;
			grad_c -= grad_c_off * y_c;
			y_c = 0;
		}
		x_b <<= 16;
		grad_b <<= 16;
		if (y_b < 0) {
			x_b -= x_a_off * y_b;
			grad_b -= grad_a_off * y_b;
			y_b = 0;
		}
		int l9 = y_c - centerY;
		l4 += j5 * l9;
		k5 += i6 * l9;
		j6 += l6 * l9;
		if (x_b_off < x_c_off) {
			y_a -= y_b;
			y_b -= y_c;
			y_c = lineOffsets[y_c];
			while (--y_b >= 0) {
				drawTexturedLine(pixels, texture, y_c, x_a >> 16, x_c >> 16, grad_a >> 8, grad_c >> 8, l4, k5, j6, i5, l5, k6);
				x_a += x_b_off;
				x_c += x_c_off;
				grad_a += grad_b_off;
				grad_c += grad_c_off;
				y_c += width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y_a >= 0) {
				drawTexturedLine(pixels, texture, y_c, x_b >> 16, x_c >> 16, grad_b >> 8, grad_c >> 8, l4, k5, j6, i5, l5, k6);
				x_b += x_a_off;
				x_c += x_c_off;
				grad_b += grad_a_off;
				grad_c += grad_c_off;
				y_c += width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			return;
		}
		y_a -= y_b;
		y_b -= y_c;
		y_c = lineOffsets[y_c];
		while (--y_b >= 0) {
			drawTexturedLine(pixels, texture, y_c, x_c >> 16, x_a >> 16, grad_c >> 8, grad_a >> 8, l4, k5, j6, i5, l5, k6);
			x_a += x_b_off;
			x_c += x_c_off;
			grad_a += grad_b_off;
			grad_c += grad_c_off;
			y_c += width;
			l4 += j5;
			k5 += i6;
			j6 += l6;
		}
		while (--y_a >= 0) {
			drawTexturedLine(pixels, texture, y_c, x_c >> 16, x_b >> 16, grad_c >> 8, grad_b >> 8, l4, k5, j6, i5, l5, k6);
			x_b += x_a_off;
			x_c += x_c_off;
			grad_b += grad_a_off;
			grad_c += grad_c_off;
			y_c += width;
			l4 += j5;
			k5 += i6;
			j6 += l6;
		}
	}

	private static void drawTexturedLine(int dest[], int texture[], int dest_off, int start_x, int end_x, int color_index,
	                                     int gradient, int arg7, int arg8, int arg9, int arg10, int arg11, int arg12) {
		int rgb = 0;
		int loops = 0;
		if (start_x >= end_x) {
			return;
		}
		int j3;
		int k3;
		if (restrict_edges) {
			j3 = (gradient - color_index) / (end_x - start_x);
			if (end_x > viewportRx) {
				end_x = viewportRx;
			}
			if (start_x < 0) {
				color_index -= start_x * j3;
				start_x = 0;
			}
			if (start_x >= end_x) {
				return;
			}
			k3 = end_x - start_x >> 3;
			j3 <<= 12;
			color_index <<= 9;
		} else {
			if (end_x - start_x > 7) {
				k3 = end_x - start_x >> 3;
				j3 = (gradient - color_index) * anIntArray1468[k3] >> 6;
			} else {
				k3 = 0;
				j3 = 0;
			}
			color_index <<= 9;
		}
		dest_off += start_x;
		if (lowMem) {
			int i4 = 0;
			int k4 = 0;
			int k6 = start_x - centerX;
			arg7 += (arg10 >> 3) * k6;
			arg8 += (arg11 >> 3) * k6;
			arg9 += (arg12 >> 3) * k6;
			int i5 = arg9 >> 12;
			if (i5 != 0) {
				rgb = arg7 / i5;
				loops = arg8 / i5;
				if (rgb < 0) {
					rgb = 0;
				} else if (rgb > 4032) {
					rgb = 4032;
				}
			}
			arg7 += arg10;
			arg8 += arg11;
			arg9 += arg12;
			i5 = arg9 >> 12;
			if (i5 != 0) {
				i4 = arg7 / i5;
				k4 = arg8 / i5;
				if (i4 < 7) {
					i4 = 7;
				} else if (i4 > 4032) {
					i4 = 4032;
				}
			}
			int i7 = i4 - rgb >> 3;
			int k7 = k4 - loops >> 3;
			rgb += (color_index & 0x600000) >> 3;
			int i8 = color_index >> 23;
			if (opaque) {
				while (k3-- > 0) {
					dest[dest_off++] = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8;
					rgb += i7;
					loops += k7;
					dest[dest_off++] = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8;
					rgb += i7;
					loops += k7;
					dest[dest_off++] = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8;
					rgb += i7;
					loops += k7;
					dest[dest_off++] = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8;
					rgb += i7;
					loops += k7;
					dest[dest_off++] = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8;
					rgb += i7;
					loops += k7;
					dest[dest_off++] = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8;
					rgb += i7;
					loops += k7;
					dest[dest_off++] = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8;
					rgb += i7;
					loops += k7;
					dest[dest_off++] = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8;
					rgb = i4;
					loops = k4;
					arg7 += arg10;
					arg8 += arg11;
					arg9 += arg12;
					int j5 = arg9 >> 12;
					if (j5 != 0) {
						i4 = arg7 / j5;
						k4 = arg8 / j5;
						if (i4 < 7) {
							i4 = 7;
						} else if (i4 > 4032) {
							i4 = 4032;
						}
					}
					i7 = i4 - rgb >> 3;
					k7 = k4 - loops >> 3;
					color_index += j3;
					rgb += (color_index & 0x600000) >> 3;
					i8 = color_index >> 23;
				}
				for (k3 = end_x - start_x & 7; k3-- > 0; ) {
					dest[dest_off++] = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8;
					rgb += i7;
					loops += k7;
				}

				return;
			}
			while (k3-- > 0) {
				int k8;
				if ((k8 = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8) != 0) {
					dest[dest_off] = k8;
				}
				dest_off++;
				rgb += i7;
				loops += k7;
				if ((k8 = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8) != 0) {
					dest[dest_off] = k8;
				}
				dest_off++;
				rgb += i7;
				loops += k7;
				if ((k8 = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8) != 0) {
					dest[dest_off] = k8;
				}
				dest_off++;
				rgb += i7;
				loops += k7;
				if ((k8 = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8) != 0) {
					dest[dest_off] = k8;
				}
				dest_off++;
				rgb += i7;
				loops += k7;
				if ((k8 = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8) != 0) {
					dest[dest_off] = k8;
				}
				dest_off++;
				rgb += i7;
				loops += k7;
				if ((k8 = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8) != 0) {
					dest[dest_off] = k8;
				}
				dest_off++;
				rgb += i7;
				loops += k7;
				if ((k8 = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8) != 0) {
					dest[dest_off] = k8;
				}
				dest_off++;
				rgb += i7;
				loops += k7;
				if ((k8 = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8) != 0) {
					dest[dest_off] = k8;
				}
				dest_off++;
				rgb = i4;
				loops = k4;
				arg7 += arg10;
				arg8 += arg11;
				arg9 += arg12;
				int k5 = arg9 >> 12;
				if (k5 != 0) {
					i4 = arg7 / k5;
					k4 = arg8 / k5;
					if (i4 < 7) {
						i4 = 7;
					} else if (i4 > 4032) {
						i4 = 4032;
					}
				}
				i7 = i4 - rgb >> 3;
				k7 = k4 - loops >> 3;
				color_index += j3;
				rgb += (color_index & 0x600000) >> 3;
				i8 = color_index >> 23;
			}
			for (k3 = end_x - start_x & 7; k3-- > 0; ) {
				int l8;
				if ((l8 = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8) != 0) {
					dest[dest_off] = l8;
				}
				dest_off++;
				rgb += i7;
				loops += k7;
			}

			return;
		}
		int j4 = 0;
		int l4 = 0;
		int l6 = start_x - centerX;
		arg7 += (arg10 >> 3) * l6;
		arg8 += (arg11 >> 3) * l6;
		arg9 += (arg12 >> 3) * l6;
		int l5 = arg9 >> 14;
		if (l5 != 0) {
			rgb = arg7 / l5;
			loops = arg8 / l5;
			if (rgb < 0) {
				rgb = 0;
			} else if (rgb > 16256) {
				rgb = 16256;
			}
		}
		arg7 += arg10;
		arg8 += arg11;
		arg9 += arg12;
		l5 = arg9 >> 14;
		if (l5 != 0) {
			j4 = arg7 / l5;
			l4 = arg8 / l5;
			if (j4 < 7) {
				j4 = 7;
			} else if (j4 > 16256) {
				j4 = 16256;
			}
		}
		int j7 = j4 - rgb >> 3;
		int l7 = l4 - loops >> 3;
		rgb += color_index & 0x600000;
		int j8 = color_index >> 23;
		if (opaque) {
			while (k3-- > 0) {
				dest[dest_off++] = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8;
				rgb += j7;
				loops += l7;
				dest[dest_off++] = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8;
				rgb += j7;
				loops += l7;
				dest[dest_off++] = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8;
				rgb += j7;
				loops += l7;
				dest[dest_off++] = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8;
				rgb += j7;
				loops += l7;
				dest[dest_off++] = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8;
				rgb += j7;
				loops += l7;
				dest[dest_off++] = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8;
				rgb += j7;
				loops += l7;
				dest[dest_off++] = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8;
				rgb += j7;
				loops += l7;
				dest[dest_off++] = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8;
				rgb = j4;
				loops = l4;
				arg7 += arg10;
				arg8 += arg11;
				arg9 += arg12;
				int i6 = arg9 >> 14;
				if (i6 != 0) {
					j4 = arg7 / i6;
					l4 = arg8 / i6;
					if (j4 < 7) {
						j4 = 7;
					} else if (j4 > 16256) {
						j4 = 16256;
					}
				}
				j7 = j4 - rgb >> 3;
				l7 = l4 - loops >> 3;
				color_index += j3;
				rgb += color_index & 0x600000;
				j8 = color_index >> 23;
			}
			for (k3 = end_x - start_x & 7; k3-- > 0; ) {
				dest[dest_off++] = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8;
				rgb += j7;
				loops += l7;
			}

			return;
		}
		while (k3-- > 0) {
			int i9;
			if ((i9 = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8) != 0) {
				dest[dest_off] = i9;
			}
			dest_off++;
			rgb += j7;
			loops += l7;
			if ((i9 = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8) != 0) {
				dest[dest_off] = i9;
			}
			dest_off++;
			rgb += j7;
			loops += l7;
			if ((i9 = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8) != 0) {
				dest[dest_off] = i9;
			}
			dest_off++;
			rgb += j7;
			loops += l7;
			if ((i9 = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8) != 0) {
				dest[dest_off] = i9;
			}
			dest_off++;
			rgb += j7;
			loops += l7;
			if ((i9 = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8) != 0) {
				dest[dest_off] = i9;
			}
			dest_off++;
			rgb += j7;
			loops += l7;
			if ((i9 = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8) != 0) {
				dest[dest_off] = i9;
			}
			dest_off++;
			rgb += j7;
			loops += l7;
			if ((i9 = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8) != 0) {
				dest[dest_off] = i9;
			}
			dest_off++;
			rgb += j7;
			loops += l7;
			if ((i9 = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8) != 0) {
				dest[dest_off] = i9;
			}
			dest_off++;
			rgb = j4;
			loops = l4;
			arg7 += arg10;
			arg8 += arg11;
			arg9 += arg12;
			int j6 = arg9 >> 14;
			if (j6 != 0) {
				j4 = arg7 / j6;
				l4 = arg8 / j6;
				if (j4 < 7) {
					j4 = 7;
				} else if (j4 > 16256) {
					j4 = 16256;
				}
			}
			j7 = j4 - rgb >> 3;
			l7 = l4 - loops >> 3;
			color_index += j3;
			rgb += color_index & 0x600000;
			j8 = color_index >> 23;
		}
		for (int l3 = end_x - start_x & 7; l3-- > 0; ) {
			int j9;
			if ((j9 = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8) != 0) {
				dest[dest_off] = j9;
			}
			dest_off++;
			rgb += j7;
			loops += l7;
		}

	}

	//public static final int anInt1459 = -477; dummy never used
	public static boolean lowMem = true;
	static boolean restrict_edges;
	private static boolean opaque;
	public static boolean aBoolean1464 = true;
	public static int alpha;
	public static int centerX;
	public static int centerY;
	private static int[] anIntArray1468;
	public static final int[] anIntArray1469;
	public static int sineTable[];
	public static int cosineTable[];
	public static int lineOffsets[];
	private static int loadedTextureCount;
	public static IndexedImage textureImages[] = new IndexedImage[50];
	private static boolean[] textureIsTransparent = new boolean[50];
	private static int[] averageTextureColour = new int[50];
	private static int textureTexelPoolPointer;
	private static int[][] texelArrayPool;
	private static int[][] texelCache = new int[50][];
	public static int textureLastUsed[] = new int[50];
	public static int textureGetCount;
	public static int hsl2rgb[] = new int[0x10000];
	private static int[][] texturePalettes = new int[50][];

	static {
		anIntArray1468 = new int[512];
		anIntArray1469 = new int[2048];
		sineTable = new int[2048];
		cosineTable = new int[2048];
		for (int i = 1; i < 512; i++) {
			anIntArray1468[i] = 32768 / i;
		}

		for (int i = 1; i < 2048; i++) {
			anIntArray1469[i] = 0x10000 / i;
		}

		for (int i = 0; i < 2048; i++) {
			sineTable[i] = (int) (65536D * Math.sin((double) i * 0.0030679614999999999D));
			cosineTable[i] = (int) (65536D * Math.cos((double) i * 0.0030679614999999999D));
		}

	}
}