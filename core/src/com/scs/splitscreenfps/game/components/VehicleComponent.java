package com.scs.splitscreenfps.game.components;

public class VehicleComponent {

	public float current_speed;
	public float angle_rads;
	public int playerId;

	public VehicleComponent(int _playerId) {
		playerId = _playerId;
	}
}
