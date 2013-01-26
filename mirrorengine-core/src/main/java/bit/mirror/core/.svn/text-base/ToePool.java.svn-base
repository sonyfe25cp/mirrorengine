package bit.mirror.core;

import java.util.ArrayList;
import java.util.List;

public class ToePool implements Lifecycle {
	private Coordinator coordinator = null;
	private int size = 25;

	public Coordinator getCoordinator() {
		return coordinator;
	}

	public void setCoordinator(Coordinator coordinator) {
		this.coordinator = coordinator;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;

	}

	private List<ToeThread> toes = new ArrayList<ToeThread>();

	
	public void start() {
		for(int i=0; i<size; i++) {
			ToeThread tt = new ToeThread();
			tt.setCoordinator(coordinator);
			tt.setNo(i);
			toes.add(tt);
			tt.start();
		}
	}
	
	
	public void stop() {
		for(ToeThread tt : toes) {
			tt.interrupt();
		}
	}
	
	public void join() throws InterruptedException {
		for(ToeThread tt : toes) {
			tt.join();
		}		
	}
}
