package alg;

import svm.SVM;

public class Perceptron extends Algorithm {
	private volatile boolean running = true;
	private volatile boolean suspended = false;

	public Perceptron(SVM svm){
		super(svm);
		if(svm.ind.V != null){
			name = "Perceptron";
			svm.outd.algorithm = name;
			svm.outd.showInputData();
		}
	}

	public void suspend_(){ suspended = true; }

	public void resume_(){
		suspended = false;
	}

	public void stop_(){
		running = false;
		suspended = false;
	}

	public void run(){
		t = System.currentTimeMillis();
		float[] w = new float[dim + 1];
		long stage = 0;
		boolean converged = false;
		while(running && stage < P && !converged){
			stage++;
			int errors = 0;
			for(int i = 0; i < N && running; i++){
				while(suspended){
					try { Thread.sleep(100); } catch(InterruptedException ex){}
				}
				float s = 0;
				for(int j = 0; j < dim; j++)
					s += w[j] * svm.ind.V[i].X[j];
				s += w[dim];
				int y_hat = (s >= 0) ? 1 : 0;
				int y = svm.ind.V[i].cl.Y;
				if(y_hat != y){
					errors++;
					float delta = eta * (y - y_hat);
					for(int j = 0; j < dim; j++)
						w[j] += delta * svm.ind.V[i].X[j];
					w[dim] += delta;
				}
			}
			if(errors == 0) converged = true;
			if(dim == 2) svm.design.setPointsOfLine(w);
			svm.outd.stages_count = stage;
			svm.outd.computing_time = System.currentTimeMillis() - t;
			svm.outd.w = w;
			svm.outd.accuracy = getAccuracy(w);
			svm.outd.showOutputData();
			svm.design.repaint();
		}
		svm.design.calculates = false;
		svm.design.repaint();
		svm.control.start.enable(false);
	}
}
