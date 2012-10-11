package com.zhang.waterwave;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class WaterWaveView extends SurfaceView implements SurfaceHolder.Callback{

	private int backWidth;

	private int backHeight;

	/**
	 * buf1 和 buf2是波能缓冲区，分别代表了每个点的前一时刻和后一时刻的波幅数据
	 */
	private short[] buf1;
	private short[] buf2;

	private int[] bitmap1;
	private int[] bitmap2;

	private Bitmap bgImage = null;

	private boolean firstLoad = false;

	WavingThread wavingThread = new WavingThread();

	SurfaceHolder mSurfaceHolder = null;
	
	private int doubleWidth;
	
	private int fiveWidth;
	
	private int loopTime;
	
	private int bitmapLen;

	public WaterWaveView(Context context) {
		super(context);
		mSurfaceHolder = getHolder();
		mSurfaceHolder.addCallback(this);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (!firstLoad) {
			bgImage = BitmapFactory.decodeResource(this.getResources(),R.drawable.bg);
			bgImage = Bitmap.createScaledBitmap(bgImage, w, h, false);// 缩放而已
			backWidth = bgImage.getWidth();
			backHeight = bgImage.getHeight();

			buf2 = new short[backWidth * backHeight];
			buf1 = new short[backWidth * backHeight];

			bitmap2 = new int[backWidth * backHeight];
			bitmap1 = new int[backWidth * backHeight];

			// 将bgImage的像素拷贝到bitmap1数组中，用于渲染。。。
			bgImage.getPixels(bitmap1, 0, backWidth, 0, 0, backWidth,
					backHeight);
			bgImage.getPixels(bitmap2, 0, backWidth, 0, 0, backWidth,
					backHeight);

			for (int i = 0; i < backWidth * backHeight; ++i) {
				buf2[i] = 0;
				buf1[i] = 0;
			}
			doubleWidth = backWidth << 1;
			
			fiveWidth = 5 * backWidth;
			
			loopTime = ((backHeight - 4) * backWidth) >> 1;
			
			bitmapLen = backWidth * backHeight - 1;
			
			firstLoad = true;
		}

	}

	class WavingThread extends Thread {
		boolean running = true;

		public void setRunning(boolean running) {
			this.running = running;
		}

		@Override
		public void run() {
			Canvas c = null;
			while (running) {
				c = mSurfaceHolder.lockCanvas();
				makeRipple();
				doDraw(c);
				mSurfaceHolder.unlockCanvasAndPost(c);
			}
		}
	}

	/*******************************************************
	 *计算波能数据缓冲区
	 *******************************************************/
	private void makeRipple() {
		int k = fiveWidth;
		int xoff = 0, yoff = 0;
		int cp = 0;
		
		int tarClr = 0;
		int i = fiveWidth;
		while(i < loopTime){
			
			// 波能扩散
			buf2[k] = (short) (((buf1[k - 2] + buf1[k + 2]
					+ buf1[k - doubleWidth] + buf1[k + doubleWidth]) >> 1) - buf2[k]);
			
			// 波能衰减
			buf2[k] = (short)(buf2[k] - (buf2[k] >> 5));
			
			// 求出该点的左上的那个点xoff,yoff
			cp = k - doubleWidth - 2;

			xoff = buf2[cp - 2] - buf2[cp + 2];
			yoff = buf2[cp - doubleWidth] - buf2[k - 2];

			tarClr = k + yoff * doubleWidth + xoff;
			if(tarClr > bitmapLen || tarClr < 0){
				k += 2;
				continue;
			}
			// 复制象素
			bitmap2[k] = bitmap1[tarClr];
			k += 2;
			++i;
		}

		short[] tmpBuf = buf2;
		buf2 = buf1;
		buf1 = tmpBuf;

	}

	/*****************************************************
	 * 增加波源 x坐标 y坐标 波源半径 波源能量
	 *****************************************************/
	private void touchWater(int x, int y, int stonesize, int stoneweight) {
		// 判断坐标是否在屏幕范围内
		if (x + stonesize > backWidth) {
			return;
		}
		if (y + stonesize > backHeight) {
			return;
		}
		if (x - stonesize < 0) {
			return;
		}
		if (y - stonesize < 0) {
			return;
		}
		// 产生波源，填充前导波能缓冲池
		int endStoneX = x + stonesize;
		int endStoneY = y + stonesize;
		int squaSize = stonesize * stonesize;
		int posy = y - stonesize;
		int posx = x - stonesize;
		for (posy = y - stonesize; posy < endStoneY; ++posy) {
			for (posx = x - stonesize; posx < endStoneX; ++posx) {
				if ((posx - x) * (posx - x) + (posy - y) * (posy - y) < squaSize) {
					buf1[backWidth * posy + posx] = (short) -stoneweight;
				}
			}
		}

	}

	/*****************************************************
	 * 增加波源 x坐标 y坐标 波源半径 波源能量
	 *****************************************************/
	private void trickWater(int x, int y, int stonesize, int stoneweight) {
		// 判断坐标是否在屏幕范围内
		if (x + stonesize > backWidth) {
			return;
		}
		if (y + stonesize > backHeight) {
			return;
		}
		if (x - stonesize < 0) {
			return;
		}
		if (y - stonesize < 0) {
			return;
		}

		// 产生波源，填充前导波能缓冲池
		int endStoneX = x + stonesize;
		int endStoneY = y + stonesize;
		int posy = y - stonesize;
		int posx = x - stonesize;
		for (posy = y - stonesize; posy < endStoneY; ++posy) {
			for (posx = x - stonesize; posx < endStoneX; ++posx) {
				if (posy >= 0 && posy < backHeight && posx >= 0
						&& posx < backWidth) {
					buf1[backWidth * posy + posx] = (short) -stoneweight;
				}
			}
		}

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			touchWater((int) event.getX(), (int) event.getY(), 4, 160);
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			trickWater((int) event.getX(), (int) event.getY(), 2, 64);
		}
		return true;
	}

	protected void doDraw(Canvas canvas) {
		canvas.drawBitmap(bitmap2, 0, backWidth, 0, 0, backWidth, backHeight,
				false, null);

	}
	
	

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		wavingThread.setRunning(true);
		wavingThread.start();
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		wavingThread.setRunning(false);
		// 非暴力关闭线程，直到此次该线程运行结束之前，主线程停止运行，以防止Surface被重新激活
        while (retry) {
            try {
            	wavingThread.join();       //阻塞current Thread(当前执行线程)直到被调用线程(thread)完成。
                retry = false;
            } catch (InterruptedException e) {
            }
        }
		
	}

}
