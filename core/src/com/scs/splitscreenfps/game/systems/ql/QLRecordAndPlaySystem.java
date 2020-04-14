package com.scs.splitscreenfps.game.systems.ql;

import java.util.LinkedList;

import com.scs.basicecs.AbstractEntity;
import com.scs.basicecs.AbstractSystem;
import com.scs.basicecs.BasicECS;
import com.scs.splitscreenfps.game.components.PositionComponent;
import com.scs.splitscreenfps.game.components.ql.IsRecordable;
import com.scs.splitscreenfps.game.levels.QuantumLeagueLevel;

public class QLRecordAndPlaySystem extends AbstractSystem {

	private QuantumLeagueLevel level;
	private LinkedList<EntityRecordData> thisPhaseRecordData = new LinkedList<EntityRecordData>();
	private LinkedList<EntityRecordData> prevPhaseRecordData = new LinkedList<EntityRecordData>();
	private long currentPhaseTime;

	public QLRecordAndPlaySystem(BasicECS _ecs, QuantumLeagueLevel _level) {
		super(_ecs, IsRecordable.class);

		level = _level;
	}


	public void loadNewRecordData() {
		this.prevPhaseRecordData = this.thisPhaseRecordData;
		this.thisPhaseRecordData.clear();
	}


	@Override
	public void process() {
		currentPhaseTime = level.getPhaseTime();

		super.process();

		// Play from prev recording
		if (this.prevPhaseRecordData.size() > 0) {
			EntityRecordData next = this.prevPhaseRecordData.getFirst();
			while (next.time < this.currentPhaseTime) {
				next = this.prevPhaseRecordData.removeFirst();
				moveEntity(next);
				if (this.prevPhaseRecordData.size() == 0) {
					break;
				}
				next = this.prevPhaseRecordData.getFirst();
			}
		}
	}


	private void moveEntity(EntityRecordData data) {
		if (data.cmd == EntityRecordData.CMD_MOVED) {
			AbstractEntity entity = ecs.get(data.entityId);
			PositionComponent posData = (PositionComponent)entity.getComponent(PositionComponent.class);
			posData.position.set(data.position);
		} else if (data.cmd == EntityRecordData.CMD_CREATED) {
			// todo
		} else if (data.cmd == EntityRecordData.CMD_DESTROYED) {
			AbstractEntity entity = ecs.get(data.entityId);
			IsRecordable isRecordable = (IsRecordable)entity.getComponent(IsRecordable.class);
			isRecordable.active = false;
			entity.remove();
		}
	}


	@Override
	public void processEntity(AbstractEntity entity) {
		if (level.isGamePhase()) {
			IsRecordable isRecordable = (IsRecordable)entity.getComponent(IsRecordable.class);
			if (isRecordable.active) {
				PositionComponent posData = (PositionComponent)entity.getComponent(PositionComponent.class);
				EntityRecordData data = new EntityRecordData();
				data.entityId = entity.entityId;
				data.cmd = EntityRecordData.CMD_MOVED;
				data.time = currentPhaseTime;
				data.position.set(posData.position);

				this.thisPhaseRecordData.add(data);
			}
		}
	}

}
