package com.vuphone.tictactoe;

import com.vuphone.tictactoe.model.geometry.Point;

public interface BoardGLViewDelegate {

	void paintSurfaceSquareTouched(int x, int y);

	void paintSurfaceGLReady();

	void paintSurfaceGameClose();

}
