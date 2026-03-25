package tools;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import svm.SVM;

public class InputDataGenerator extends Dialog{
	SVM svm;
	TextField attributes_count, vectors_count, min, max, classes_count, mg;
	Checkbox liniar, als, cs, rs;
	Label attributes_count_label, vectors_count_label, min_label, max_label, classes_count_label, liniar_label;
	Label mg_label, als_label, cs_label, rs_label;
	Button generate, save;
	public TextArea ta;
	String dir = ".\\svm\\data", path;

	public InputDataGenerator(SVM svm){
		super(svm, "Input Data Generator", true);
		this.svm = svm;
		setBackground(svm.settings.background_color_default);
		setResizable(false);
		resize(640,480);
		move((svm.res.width-640)/2,(svm.res.height-480)/2);
		setLayout(null);

		int x1 = 10, x2 = 380;
		int y = 35;
		attributes_count_label = new Label("Attributes Count:");
		attributes_count_label.setBounds(x1,y,150,20);
		attributes_count_label.setForeground(Color.white);
		add(attributes_count_label);
		attributes_count = new TextField("2");
		attributes_count.setBounds(x1+150,y,100,20);
		add(attributes_count);

		vectors_count_label = new Label("Vectors Count:");
		vectors_count_label.setBounds(x2,y,150,20);
		vectors_count_label.setForeground(Color.white);
		add(vectors_count_label);
		vectors_count = new TextField("1000");
		vectors_count.setBounds(x2+150,y,100,20);
		add(vectors_count);

		y+=30;
		min_label = new Label("Minimum Coordinates:");
		min_label.setBounds(x1,y,150,20);
		min_label.setForeground(Color.white);
		add(min_label);
		min = new TextField("-1000");
		min.setBounds(x1+150,y,100,20);
		add(min);

		max_label = new Label("Maximum Coordinates:");
		max_label.setBounds(x2,y,150,20);
		max_label.setForeground(Color.white);
		add(max_label);
		max = new TextField("1000");
		max.setBounds(x2+150,y,100,20);
		add(max);

		y+=30;
		classes_count_label = new Label("Classes Count:");
		classes_count_label.setBounds(x1,y,150,20);
		classes_count_label.setForeground(Color.white);
		add(classes_count_label);
		classes_count = new TextField("2");
		classes_count.setBounds(x1+150,y,100,20);
		add(classes_count);
		classes_count.enable(false);

		liniar_label = new Label("Liniar separated:");
		liniar_label.setBounds(x2,y,150,20);
		liniar_label.setForeground(Color.white);
		add(liniar_label);
		liniar = new Checkbox("");
		liniar.setBounds(x2+150,y,20,20);
		liniar.setState(true);
		add(liniar);

		y+=30;
		mg_label = new Label("Margin:");
		mg_label.setBounds(x1,y,70,20);
		mg_label.setForeground(Color.white);
		add(mg_label);
		mg = new TextField("50");
		mg.setBounds(x1+70,y,80,20);
		add(mg);

		als_label = new Label("Almost liniar separated:");
		als_label.setBounds(x1+170,y,200,20);
		als_label.setForeground(Color.white);
		add(als_label);
		als = new Checkbox("");
		als.setBounds(x1+370,y,20,20);
		add(als);

		y+=30;
		cs_label = new Label("Circular separated:");
		cs_label.setBounds(x1,y,160,20);
		cs_label.setForeground(Color.white);
		add(cs_label);
		cs = new Checkbox("");
		cs.setBounds(x1+160,y,20,20);
		add(cs);

		rs_label = new Label("Random separated:");
		rs_label.setBounds(x1+200,y,160,20);
		rs_label.setForeground(Color.white);
		add(rs_label);
		rs = new Checkbox("");
		rs.setBounds(x1+360,y,20,20);
		add(rs);

		y+=30;
		generate = new Button("Generate");
		generate.setBounds(x1,y,250,20);
		generate.setBackground(svm.settings.button_color_default);
		generate.setForeground(svm.settings.button_label_default);
		add(generate);

		save = new Button("Save");
		save.setBounds(x2,y,250,20);
		save.setBackground(svm.settings.button_color_default);
		save.setForeground(svm.settings.button_label_default);
		add(save);

		y+=30;
		ta = new TextArea("");
		ta.setBounds(x1,y,size().width-2*x1,size().height-y-x1);
		ta.setBackground(svm.settings.button_color_default);
		ta.setForeground(svm.settings.string_color_default);
		add(ta);

		show();
	}

	public boolean handleEvent(Event e){
		if(e.id==Event.WINDOW_DESTROY){
			dispose();
		}else if(e.id==Event.ACTION_EVENT && e.target == generate){
			generateData();
           	return true;
		}else if(e.id==Event.ACTION_EVENT && e.target == save){
			saveGeneratedData();
           	return true;
		}else if(e.id==Event.ACTION_EVENT && e.target == liniar){
			if(liniar.getState()){
				als.setState(false);
				cs.setState(false);
				rs.setState(false);
				classes_count.setText("2");
				classes_count.enable(false);
			}
           	return true;
		}else if(e.id==Event.ACTION_EVENT && e.target == als){
			if(als.getState()){
				liniar.setState(false);
				cs.setState(false);
				rs.setState(false);
				classes_count.setText("2");
				classes_count.enable(false);
			}
           	return true;
		}else if(e.id==Event.ACTION_EVENT && e.target == cs){
			if(cs.getState()){
				liniar.setState(false);
				als.setState(false);
				rs.setState(false);
				classes_count.setText("2");
				classes_count.enable(false);
			}
           	return true;
		}else if(e.id==Event.ACTION_EVENT && e.target == rs){
			if(rs.getState()){
				liniar.setState(false);
				als.setState(false);
				cs.setState(false);
				classes_count.enable(true);
			}
           	return true;
		}else return false;
		return super.handleEvent(e);
	}

	void generateData(){
		ta.setText("");
		if(liniar.getState())
			generateLiniarData();
		else if(als.getState())
			generateAlmostLiniarData();
		else if(cs.getState())
			generateCircularData();
		else
			generateRandomData();
	}

	void generateRandomData(){
		int N = Integer.parseInt(vectors_count.getText());
		int n = Integer.parseInt(attributes_count.getText());
		int MIN = Integer.parseInt(min.getText());
		int MAX = Integer.parseInt(max.getText());
		int C = Integer.parseInt(classes_count.getText());
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%\n");
		ta.append("% attributes count = " + n + "\n");
		ta.append("% vectors count = " + N + "\n");
		ta.append("%\n");
		ta.append("% Edit the attribute names (attrib_1, attrib_2, ...) in this text box and then save.\n");
		ta.append("%\n");
		for(int i=1; i<=n; i++)
			ta.append("@attribute attrib_" + i + " numeric\n");
		String ss = "";
		for(int i=0; i<C-1; i++)
			ss += i + ", ";
		ss += (C-1);
		ta.append("@attribute class {" + ss + "}\n");
		ta.append("@data\n");
		if(N > 1 && n > 1 && MIN < MAX && C > 1){
			for(int k=0; k<N; k++){
				String s = "";
				for(int i=0; i<n; i++){
					s += (MIN + (float)Math.random()*(MAX-MIN)) + ",";
				}
				s += (int)(Math.random()*C) + "\n";
				ta.append(s);
			}
		}else mesaj();
	}

	void generateLiniarData(){
		int N = Integer.parseInt(vectors_count.getText());
		int n = Integer.parseInt(attributes_count.getText());
		int MIN = Integer.parseInt(min.getText());
		int MAX = Integer.parseInt(max.getText());
		int MG = Integer.parseInt(mg.getText());
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%\n");
		ta.append("% attributes count = " + n + "\n");
		ta.append("% vectors count = " + N + "\n");
		ta.append("%\n");
		ta.append("% Edit the attribute names (attrib_1, attrib_2, ...) in this text box and then save.\n");
		ta.append("%\n");
		for(int i=1; i<=n; i++)
			ta.append("@attribute attrib_" + i + " numeric\n");
		ta.append("@attribute class {0, 1}\n");
		ta.append("@data\n");
		if(N > 1 && n > 1 && MIN < MAX){
			float[] w = new float[n+1];
			float maxVal = -10000000000f;
			for(int i=0; i<n; i++) {
				w[i] = MIN + (float)Math.random()*(MAX-MIN);
				if(w[i] > maxVal) maxVal = w[i];
			}
			w[n] = maxVal*(MIN + (float)Math.random()*(MAX-MIN));
			int k = 0, attempts = 0;
			while(k < N && attempts < N*100){
				attempts++;
				float[] x = new float[n];
				String s = "";
				for(int i=0; i<n; i++){
					x[i] = MIN + (float)Math.random()*(MAX-MIN);
					s += x[i] + ",";
				}
				float z = 0;
				for(int i=0; i<n; i++) z += w[i]*x[i];
				z += w[n];
				if(Math.abs(z) < MG) continue;
				int y = (z >= 0) ? 1 : 0;
				ta.append(s + y + "\n");
				k++;
			}
			if(k < N) mesaj();
		}else mesaj();
	}

	void generateAlmostLiniarData(){
		int N = Integer.parseInt(vectors_count.getText());
		int n = Integer.parseInt(attributes_count.getText());
		int MIN = Integer.parseInt(min.getText());
		int MAX = Integer.parseInt(max.getText());
		int MG = Integer.parseInt(mg.getText());
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%\n");
		ta.append("% attributes count = " + n + "\n");
		ta.append("% vectors count = " + N + "\n");
		ta.append("%\n");
		ta.append("% Edit the attribute names (attrib_1, attrib_2, ...) in this text box and then save.\n");
		ta.append("%\n");
		for(int i=1; i<=n; i++)
			ta.append("@attribute attrib_" + i + " numeric\n");
		ta.append("@attribute class {0, 1}\n");
		ta.append("@data\n");
		if(N > 1 && n > 1 && MIN < MAX){
			float[] w = new float[n+1];
			float maxVal = -10000000000f;
			for(int i=0; i<n; i++) {
				w[i] = MIN + (float)Math.random()*(MAX-MIN);
				if(w[i] > maxVal) maxVal = w[i];
			}
			w[n] = maxVal*(MIN + (float)Math.random()*(MAX-MIN));
			for(int k=0; k<N; k++){
				float[] x = new float[n];
				String s = "";
				for(int i=0; i<n; i++){
					x[i] = MIN + (float)Math.random()*(MAX-MIN);
					s += x[i] + ",";
				}
				float z = 0;
				for(int i=0; i<n; i++) z += w[i]*x[i];
				z += w[n];
				int y;
				if(Math.abs(z) < MG)
					y = (z >= 0) ? 0 : 1;
				else
					y = (z >= 0) ? 1 : 0;
				ta.append(s + y + "\n");
			}
		}else mesaj();
	}

	void generateCircularData(){
		int N = Integer.parseInt(vectors_count.getText());
		int n = Integer.parseInt(attributes_count.getText());
		int MIN = Integer.parseInt(min.getText());
		int MAX = Integer.parseInt(max.getText());
		int MG = Integer.parseInt(mg.getText());
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%............comments............\n");
		ta.append("%\n");
		ta.append("% attributes count = " + n + "\n");
		ta.append("% vectors count = " + N + "\n");
		ta.append("%\n");
		ta.append("% Edit the attribute names (attrib_1, attrib_2, ...) in this text box and then save.\n");
		ta.append("%\n");
		for(int i=1; i<=n; i++)
			ta.append("@attribute attrib_" + i + " numeric\n");
		ta.append("@attribute class {0, 1}\n");
		ta.append("@data\n");
		if(N > 1 && n > 1 && MIN < MAX && MG > 0){
			float center = (MIN + MAX) / 2.0f;
			for(int k=0; k<N; k++){
				float[] x = new float[n];
				String s = "";
				double dist = 0;
				for(int i=0; i<n; i++){
					x[i] = MIN + (float)Math.random()*(MAX-MIN);
					s += x[i] + ",";
					dist += (x[i]-center)*(x[i]-center);
				}
				dist = Math.sqrt(dist);
				int y = (dist < MG) ? 0 : 1;
				ta.append(s + y + "\n");
			}
		}else mesaj();
	}


	void saveGeneratedData(){
		if(!ta.getText().equals("")){
			try{
				FileDialog fd=new FileDialog(this, "Save Generated Input Data", 1);
				if(dir!=null) fd.setDirectory(dir);
				fd.setFile("*.csv");
				fd.setVisible(true);
				if(fd.getFile() != null) {
					dir = fd.getDirectory();
					String fisier = fd.getFile();
					path = dir + fisier;
					File file = new File(path);
					BufferedWriter bw = null;
					if(file.exists()) file.delete();
					try{
						bw = new BufferedWriter(new FileWriter(file));
						bw.write(ta.getText());
						bw.close();
					}
					catch(IOException e){e.printStackTrace();}
				}
			}
			catch(Exception e) {e.printStackTrace();}
		}
	}

	void mesaj(){
		System.out.println("Vectors Dimension must be > 1.");
		System.out.println("Vectors Count must be > 1.");
		System.out.println("It is necessary that Minimum Coordonates < Maximum Coordonates.");
	}

}
