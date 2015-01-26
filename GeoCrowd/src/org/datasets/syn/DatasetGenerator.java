package org.datasets.syn;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.datasets.syn.dtype.DataTypeEnum;
import org.datasets.syn.dtype.Point;
import org.datasets.syn.dtype.Rectangle;
import org.datasets.syn.dtype.ValueFreq;
import org.datasets.syn.dtype.WeightedPoint;
import org.datasets.syn.dtype.Range;
import org.geocrowd.common.utils.MurmurHash;
import org.geocrowd.common.utils.Stats;
import org.geocrowd.common.utils.Utils;

/**
 * Generate various type of dataset, such as uniform distribution data,
 * charminar data, zipf distribution data, and sampling data
 * 
 * @author HT186010
 * 
 */
public class DatasetGenerator {
	public static int time = 0;
	public static int gaussianCluster = 4;
	public static ArrayList<Long> seeds;

	private String filePath = "";
	private Character delimiter = '\t';

	public DatasetGenerator() {
	}
	
	public DatasetGenerator(String filePath) {
		this.filePath = filePath;
	}

	/**
	 * Generate uniform points
	 * 
	 * @param n
	 * @param boundary
	 * @return
	 */
	public Vector<Point> generateUniformPoints(int n, Rectangle boundary) {
		Vector<Point> points = new Vector<Point>();

		for (int i = 0; i < n; i++) {
			Point point = UniformGenerator.randomPoint(boundary, false);
			points.add(point);
		}

		return points;
	}

	/**
	 * Each Gaussian cluster has n/gaussianCluster data points
	 * 
	 * @param n
	 * @param boundary
	 * @return
	 */
	private Vector<Point> generateMultivarDataset(int n, Rectangle boundary) {
		Vector<Point> points = new Vector<Point>();
		if (n == 0)
			return points;
		
		for (int c = 0; c < gaussianCluster; c++) {
			Point mPoint = UniformGenerator.randomPoint(boundary, false,
					seeds.get(c) + time);
			double[] means = { mPoint.getX(), mPoint.getY() };
			// mPoint.debug();
			double[][] covariances = { { boundary.getHighPoint().getX(), 0 },
					{ 0, boundary.getHighPoint().getY() } };

			MultivariateNormalDistribution mvd = new MultivariateNormalDistribution(
					means, covariances);
			int samples = 0;
			if (c == gaussianCluster - 1)
				samples = n - ((int) (n / gaussianCluster))
						* (gaussianCluster - 1);
			else
				samples = n / gaussianCluster;
			if (samples == 0)
				continue;
			double[][] data = mvd.sample(samples);
			for (int i = 0; i < samples; i++) {
				Point point = new Point(data[i][0], data[i][1]);
				points.add(point);
			}
		}
		return points;
	}

	/**
	 * Generate random weighted point
	 * 
	 * @param n
	 * @param boundary
	 * @param weightRange
	 * @param isDistinct
	 * @return
	 */
	private Vector<WeightedPoint> generateRandomWeightedPoints(int n,
			Rectangle boundary, Range weightRange, boolean isInteger,
			boolean isDistinct) {
		Vector<WeightedPoint> points = new Vector<WeightedPoint>();
		HashSet<Long> hash = new HashSet<Long>();
		int num = 0;
		while (true) {
			Point point = UniformGenerator.randomPoint(boundary, isInteger);
			String keyString = Utils.createKeyString(point);
			long hashedKey = MurmurHash.hash64(keyString);
			if (!hash.contains(hashedKey) || !isDistinct) {
				hash.add(hashedKey);
				// random distribution in weight
				double weight = 0.0;
				weight = UniformGenerator.randomValue(weightRange, true);
				points.add(new WeightedPoint(point.getX(), point.getY(), weight));
				num++;
				if (num > n)
					return points;
			}
		}
	}

	/**
	 * Generate zipf weighted point
	 * 
	 * @param n
	 * @param boundary
	 * @param weightRange
	 * @return
	 */
	private Vector<WeightedPoint> generateZipfWeightedPoints(int n,
			Rectangle boundary, Range weightRange, boolean isInteger,
			boolean isDistinct) {
		// TODO Auto-generated method stub
		Vector<WeightedPoint> points = new Vector<WeightedPoint>();
		HashSet<Long> hash = new HashSet<Long>();
		int num = 0;
		while (true) {
			Point point = UniformGenerator.randomPoint(boundary, isInteger);
			String keyString = Utils.createKeyString(point);
			long hashedKey = MurmurHash.hash64(keyString);
			if (!hash.contains(point) || !isDistinct) {
				hash.add(hashedKey);
				double weight = 0.0;
				// generate weight list that follows zipf distribution
				// rounding the weight to integer value
				int start = (int) Math.round(weightRange.getStart());
				int end = (int) Math.round(weightRange.getEnd());

				// create a list of count
				ZipfDistribution zipf = new ZipfDistribution(2, 1);
				Double[] probList = new Double[end - start + 1]; // probability
																	// list
																	// associate
																	// with
																	// weight
																	// list
				double[] weights = new double[end - start + 1]; // weight
																// list

				for (int i = start; i <= end; i++) {
					weights[i - start] = i; // populate a list of weights
					probList[i - start] = zipf.getProbability(i); // get the
																	// probability
					// of each weight,
					// smaller weight
					// appears more
					// often.
				}
				ProbabilityGenerator<Double> pg = new ProbabilityGenerator<Double>(
						probList);
				int idx = pg.nextValue(); // get the position of the weight
											// in
											// the weight list
				weight = weights[idx];

				points.add(new WeightedPoint(point.getX(), point.getY(), weight));
				num++;
				if (num > n)
					return points;
			}
		}
	}

	/**
	 * Generate two dimensional data that follows zipf distribution
	 * 
	 * @param n
	 * @param boundary
	 * @return
	 */
	private Vector<Point> generateZipfPoints(int n, Rectangle boundary) {
		Vector<Point> points = new Vector<Point>();
		ZipfDistribution zipf = new ZipfDistribution(2, 1);
		Random r = new Random();
		for (int i = 1; i <= n; i++) {
			double x = i * boundary.deltaX() / n
					+ boundary.getLowPoint().getX();
			r.setSeed(System.nanoTime());
			double y = zipf.getProbability(i) * r.nextDouble()
					* boundary.deltaY() + boundary.getLowPoint().getY();
			points.add(new Point(x, y));
		}

		return points;
	}

	/**
	 * A twisted version of Gaussian distribution, in which when the value is
	 * out of a range, we regenerate the value
	 * 
	 * @param count
	 * @param min
	 * @param max
	 * @param isInteger
	 * @param b
	 * @return
	 */
	private Vector<Double> generate1DGaussianValues(int count, double min,
			double max, boolean isInteger, boolean b) {
		Vector<Double> values = new Vector<Double>();

		int n = 0;
		while (true) {
			NormalDistribution nd = new NormalDistribution((max - min) / 2,
					(max - min) / 4);
			double val = nd.sample();
			if (val > max || val < min)
				continue;
			if (isInteger) {
				double tmp = (int) val;
				values.add(tmp);
			} else
				values.add(val);

			if (n++ == count)
				break;
		}

		return values;
	}

	/**
	 * Generate zipf distribution
	 * 
	 * @param n
	 * @param min
	 * @param max
	 * @param b
	 * @return
	 */
	private Vector<Double> generate1DZipfValues(int n, double min, double max,
			boolean isInteger, boolean isSorted) {
		Vector<Double> values = new Vector<Double>();

		int count = 0;
		while (true) {
			if (count++ == n)
				break;

			int rand = (int) UniformGenerator.randomValue(new Range(0, max
					- min), true);
			double val = ((max - min) * Utils.zipf_pmf(n, rand, 1));
			if (isInteger) {
				double tmp = (int) val;
				values.add(tmp);
			} else
				values.add(val);
		}

		return values;
	}

	/**
	 * Generate zipf distribution
	 * 
	 * @param n
	 * @param min
	 * @param max
	 * @param b
	 * @return
	 */
	private Vector<Double> generateOneDimZipfValues(int n, double min,
			double max, int bucket_size, boolean isInteger, boolean isRandomDist) {
		// TODO Auto-generated method stub
		Vector<Double> values = new Vector<Double>();
		double scale = (max - min) / bucket_size;
		ZipfDistribution zipf = new ZipfDistribution(2, 1);

		// create a probability list
		Double[] probList = new Double[bucket_size];
		for (int i = 1; i <= bucket_size; i++) {
			probList[i - 1] = zipf.getProbability(i);
			// System.out.println(probList[i - 1]);
		}

		ProbabilityGenerator<Double> pg = new ProbabilityGenerator<Double>(
				probList);

		int count = 0;
		while (true) {
			if (count++ == n)
				break;
			int indices = pg.nextValue(); // get the desired bucket
			int index = indices % bucket_size; // for sure

			// generate a random point in this range
			double x1 = index * scale + min;
			double x2 = (index + 1) * scale + min;
			double value = UniformGenerator
					.randomValue(new Range(x1, x2), true);

			// add the point to
			if (isInteger)
				value = Math.round(value);

			values.add(value);
		}

		// permute the values
		if (isRandomDist && isInteger) {
			int N = (int) Math.round(max - min + 1);
			int[] a = new int[N];
			for (int i = 0; i < N; i++) {
				a[i] = i;
			}
			int[] b = Utils.randomPermutation(a);

			Vector<Double> permutedValues = new Vector<Double>();
			Iterator it = values.iterator();
			while (it.hasNext()) {
				Double value = (Double) it.next();
				int value_index = (int) (value - min) % N;
				double permuted_value = b[value_index] % N + Math.floor(min);
				permutedValues.add(permuted_value);
			}
			return permutedValues;
		}

		return values;
	}

	/**
	 * 
	 * @param n
	 * @param min
	 * @param max
	 * @param isInteger
	 * @param isRandomDist
	 *            : means the frequencies are randomly permuted
	 * @param valueDistribution
	 * @return
	 */
	private Vector<ValueFreq<Double>> generateOneDimZipfFreq2(int n,
			double min, double max, boolean isInteger, boolean isRandomDist,
			int valueDistribution) {
		/*
		 * generate N values in a range that follow a distribution refer to
		 * Section 8: Experiment setup of the paper
		 * "Improved histograms for selectivity estimation of range predicates"
		 * #1: uniform #2: zipf_inc #3: zipf_dec #4: cusp_min #5 cusp_max #6:
		 * zipf_ran
		 */
		Vector<ValueFreq<Double>> valueFreqs = new Vector<ValueFreq<Double>>();
		HashSet<Double> values = null;
		switch (valueDistribution) {
		case 1: // uniform
			values = UniformGenerator.randomDistinctValues(1000, new Range(min,
					max), isInteger);
			break;
		case 2: // zipf_inc
			values = ZipfGenerator.zipfIncValues(n, isInteger);
			break;
		case 3: // zipf_dec
			values = ZipfGenerator.zipfDecValues(n, isInteger);
			break;
		case 4: // cusp_min
			values = ZipfGenerator.zipfCuspMin(n, isInteger);
			break;
		case 5: // cusp_max
			values = ZipfGenerator.zipfCuspMax(n, isInteger);
			break;
		case 6: // zipf_ran
			values = ZipfGenerator.zipfRan(n, isInteger);
			break;
		}

		Set sortedValues = new TreeSet(values);

		ZipfDistribution zipf = new ZipfDistribution(2, 1);
		double factor = 1 / zipf.getProbability(n);

		if (!isRandomDist) {
			Iterator it = sortedValues.iterator();
			for (int i = 1; i <= 1000 && it.hasNext(); i++) {
				// assign each value to its corresponding frequency
				ValueFreq vf = new ValueFreq(it.next(), (int) Math.round(zipf
						.getProbability(i) * factor));
				valueFreqs.add(vf);
			}
		} else {
			List<Integer> freqs = new Vector<Integer>();
			for (int i = 1; i <= 1000; i++) {
				freqs.add((int) Math.round(zipf.getProbability(i) * factor));
			}
			// int[] randFreqs = Utils.randomPermutation(freqs);
			Collections.shuffle(freqs);
			Iterator it = sortedValues.iterator();
			for (int i = 0; i < n && it.hasNext(); i++) {
				// assign each value to its corresponding frequency
				ValueFreq vf = new ValueFreq(it.next(), freqs.get(i));
				valueFreqs.add(vf);
			}
		}
		return valueFreqs;
	}

	/**
	 * generate charminar points
	 * 
	 * @param n
	 * @param boundary
	 * @param dim_size_x
	 * @param dim_size_y
	 * @return
	 */
	private Vector<Point> generateCharminarPoints(int n, Rectangle boundary,
			int dim_size_x, int dim_size_y) {
		Vector<Point> points = new Vector<Point>();
		double scale_x = boundary.deltaX() / dim_size_x;
		double scale_y = boundary.deltaY() / dim_size_y;

		// create a list of count
		Double[] probList = new Double[dim_size_x * dim_size_y];
		for (int j = 0; j < dim_size_y; j++)
			for (int i = 0; i < dim_size_x; i++) {
				probList[j * dim_size_x + i] = 1.0 / ((i + 2) * (j + 2));
			}
		ProbabilityGenerator<Double> pg = new ProbabilityGenerator<Double>(
				probList);

		int count = 0;
		while (true) {
			int indices = pg.nextValue();
			int index_x = indices % dim_size_x;
			int index_y = indices / dim_size_x;

			// generate a random point in this cell(index_x, index_y)
			double x1 = index_x * scale_x + boundary.getLowPoint().getX();
			double x2 = (index_x + 1) * scale_x + boundary.getLowPoint().getX();
			double y1 = index_y * scale_y + boundary.getLowPoint().getY();
			double y2 = (index_y + 1) * scale_y + boundary.getLowPoint().getY();
			points.add(UniformGenerator.randomPoint(new Rectangle(x1, y1, x2,
					y2), false));
			if (count++ == n)
				break;

			// upper left
			index_y = dim_size_y - index_y;
			x1 = index_x * scale_x + boundary.getLowPoint().getX();
			x2 = (index_x + 1) * scale_x + boundary.getLowPoint().getX();
			y1 = index_y * scale_y + boundary.getLowPoint().getY();
			y2 = (index_y + 1) * scale_y + boundary.getLowPoint().getY();
			points.add(UniformGenerator.randomPoint(new Rectangle(x1, y1, x2,
					y2), false));
			if (count++ == n)
				break;

			// upper right
			index_x = dim_size_x - index_x;
			x1 = index_x * scale_x + boundary.getLowPoint().getX();
			x2 = (index_x + 1) * scale_x + boundary.getLowPoint().getX();
			y1 = index_y * scale_y + boundary.getLowPoint().getY();
			y2 = (index_y + 1) * scale_y + boundary.getLowPoint().getY();
			points.add(UniformGenerator.randomPoint(new Rectangle(x1, y1, x2,
					y2), false));
			if (count++ == n)
				break;

			// below right
			index_y = dim_size_y - index_y;
			x1 = index_x * scale_x + boundary.getLowPoint().getX();
			x2 = (index_x + 1) * scale_x + boundary.getLowPoint().getX();
			y1 = index_y * scale_y + boundary.getLowPoint().getY();
			y2 = (index_y + 1) * scale_y + boundary.getLowPoint().getY();
			points.add(UniformGenerator.randomPoint(new Rectangle(x1, y1, x2,
					y2), false));
			if (count++ == n)
				break;

		}

		return points;
	}

	/**
	 * generate charminar weighted points
	 * 
	 * @param n
	 * @param boundary
	 * @param dim_size_x
	 * @param dim_size_y
	 * @return
	 */
	private Vector<WeightedPoint> generateCharminarWeightedPoints(int n,
			Rectangle boundary, int dim_size_x, int dim_size_y,
			boolean isInteger) {
		Vector<WeightedPoint> points = new Vector<WeightedPoint>();
		double scale_x = boundary.deltaX() / dim_size_x;
		double scale_y = boundary.deltaY() / dim_size_y;

		// create a list of count
		Double[] probList = new Double[dim_size_x * dim_size_y];
		for (int j = 0; j < dim_size_y; j++)
			for (int i = 0; i < dim_size_x; i++) {
				probList[j * dim_size_x + i] = 1.0 / ((i + 2) * (j + 2));
			}
		ProbabilityGenerator<Double> pg = new ProbabilityGenerator<Double>(
				probList);

		int count = 0;
		while (true) {
			int indices = pg.nextValue();
			int index_x = indices % dim_size_x;
			int index_y = indices / dim_size_x;

			// generate a random point in this cell(index_x, index_y)
			double x1 = index_x * scale_x + boundary.getLowPoint().getX();
			double x2 = (index_x + 1) * scale_x + boundary.getLowPoint().getX();
			double y1 = index_y * scale_y + boundary.getLowPoint().getY();
			double y2 = (index_y + 1) * scale_y + boundary.getLowPoint().getY();
			Point p1 = UniformGenerator.randomPoint(new Rectangle(x1, y1, x2,
					y2), isInteger);
			points.add(new WeightedPoint(p1.getX(), p1.getY(), 1.0));
			if (count++ == n)
				break;

			// upper left
			index_y = dim_size_y - index_y;
			x1 = index_x * scale_x + boundary.getLowPoint().getX();
			x2 = (index_x + 1) * scale_x + boundary.getLowPoint().getX();
			y1 = index_y * scale_y + boundary.getLowPoint().getY();
			y2 = (index_y + 1) * scale_y + boundary.getLowPoint().getY();
			Point p2 = UniformGenerator.randomPoint(new Rectangle(x1, y1, x2,
					y2), isInteger);
			points.add(new WeightedPoint(p1.getX(), p1.getY(), 1.0));
			if (count++ == n)
				break;

			// upper right
			index_x = dim_size_x - index_x;
			x1 = index_x * scale_x + boundary.getLowPoint().getX();
			x2 = (index_x + 1) * scale_x + boundary.getLowPoint().getX();
			y1 = index_y * scale_y + boundary.getLowPoint().getY();
			y2 = (index_y + 1) * scale_y + boundary.getLowPoint().getY();
			Point p3 = UniformGenerator.randomPoint(new Rectangle(x1, y1, x2,
					y2), isInteger);
			points.add(new WeightedPoint(p1.getX(), p1.getY(), 1.0));
			if (count++ == n)
				break;

			// below right
			index_y = dim_size_y - index_y;
			x1 = index_x * scale_x + boundary.getLowPoint().getX();
			x2 = (index_x + 1) * scale_x + boundary.getLowPoint().getX();
			y1 = index_y * scale_y + boundary.getLowPoint().getY();
			y2 = (index_y + 1) * scale_y + boundary.getLowPoint().getY();
			Point p4 = UniformGenerator.randomPoint(new Rectangle(x1, y1, x2,
					y2), isInteger);
			points.add(new WeightedPoint(p1.getX(), p1.getY(), 1.0));
			if (count++ == n)
				break;

		}

		return points;
	}

	/**
	 * simple random sampling over the whole population
	 * 
	 * @param n
	 * @param points
	 * @return
	 */
	private Vector<Point> simpleRandomSampling(int n, Vector<Point> points) {
		if (points.size() < n) {
			System.out
					.println("#points should be larger than number of samples");
			return null;
		}
		Vector<Point> sample_points = new Vector<Point>();
		Random r = new Random();
		for (int i = 0; i < n; i++) {
			r.setSeed(System.nanoTime());
			sample_points.add(points.get(r.nextInt(points.size())));
		}
		return sample_points;
	}

	/**
	 * Simple random sampling from a larger set
	 * 
	 * @param n
	 * @param outFilePath
	 */
	public void generateSamplingDataset(int n, String outFilePath) {
		PointFileReader pointFileReader = new PointFileReader(filePath);
		writePointsToFile(simpleRandomSampling(n, pointFileReader.parse()),
				outFilePath);
	}

	/**
	 * 
	 * @param n
	 * @param boundary
	 * @param uni_x_count
	 *            : the number of distinct values in x coord
	 * @param uni_y_count
	 *            : the number of distinct values in y coord
	 * @param isInteger
	 * @return
	 */
	private Vector<Point> generateNonDistinctDataset(int n, Rectangle boundary,
			int uni_x_count, int uni_y_count, boolean isInteger) {
		Vector<Point> points = new Vector<Point>();
		Vector<Double> distinct_x = UniformGenerator.randomSequence(
				uni_x_count, boundary.getLowPoint().getX(), boundary
						.getHighPoint().getX(), isInteger);
		Vector<Double> distinct_y = UniformGenerator.randomSequence(
				uni_y_count, boundary.getLowPoint().getY(), boundary
						.getHighPoint().getY(), isInteger);

		Random r = new Random();
		r.setSeed(System.nanoTime());
		for (int i = 1; i <= n; i++) {
			Point point = new Point(distinct_x.get(r.nextInt(uni_x_count)),
					distinct_y.get(r.nextInt(uni_y_count)));
			points.add(point);
		}

		return points;
	}

	/**
	 * Output is a list of values that follow a distribution
	 * 
	 * @param n
	 * @param min
	 * @param max
	 * @param dist
	 * @param isInteger
	 */
	public Vector<Double> generate1DDataset(int count, double min, double max,
			Distribution1DEnum dist, boolean isInteger) {
		Vector<Double> values = null;
		switch (dist) {
		case UNIFORM_1D: // uniform one-dimensional dataset
			values = UniformGenerator
					.randomSequence(count, min, max, isInteger);
			break;
		case ZIFFIAN_1D: // zipf distribution
			values = generate1DZipfValues(count, min, max, isInteger, false);
			break;
		case GAUSSIAN_1D:
			values = generate1DGaussianValues(count, min, max, isInteger, false);
			break;
		case TRANSFORM_FROM_2D: // create one-dimensional dataset from two
								// dimensional dataset
			values = generateOneDimFromPoints(
					"./res/dataset/twod/mcdonald.txt", isInteger);
			break;
		}
//		writeIntegersToFile(values, filePath);
		// writeIntegersToFileWithKey(values, filePath + ".data");
		
		return values;
	}

	/**
	 * Generate two-dimensional datasets
	 * 
	 * @param n
	 *            : the number of data points
	 * @param min_x
	 * @param max_x
	 * @param min_y
	 * @param max_y
	 * @param dist
	 */
	public void generate2DDataset(int n, Rectangle boundary,
			Distribution2DEnum dist) {
		Vector<Point> points = null;

		switch (dist) {
		case UNIFORM_2D: // uniform two-dimensional distribution
			points = generateUniformPoints(n, boundary);
			break;
		case ZIPFIAN_2D: // zipf two-dimensional distribution
			points = generateZipfPoints(n, boundary);
			break;
		case CHARMINAR_2D: // charminar two-dimensional dataset
			points = generateCharminarPoints(n, boundary, 100, 100);
			break;
		case UNIFORM_INT_2D: // non-distinct two-dimensional dataset
			points = generateNonDistinctDataset(n, boundary, 100, 100, true);
			break;
		case GAUSSIAN_2D: // multivariate gaussian distribution
			points = generateMultivarDataset(n, boundary);
			break;
		}
		writePointsToFile(points, filePath);
		// writePointsToFileWithKey(points, filePath + ".key.txt");
	}

	/**
	 * Generate two-dimensional dataset
	 * 
	 * @param n
	 * @param boundary
	 * @param dist
	 * @param isInteger
	 * @param smallWeight
	 * @param largeWeight
	 *            dist = 1 --> uniform distribution, dist = 2 --> zipf
	 *            distribution, dist = 3 --> charminar dataset
	 */
	public void generateWeightedDataset(int dist, int n, Rectangle boundary,
			Range weightRange, boolean isInteger, boolean isDistinct) {
		Vector<WeightedPoint> points = null;

		switch (dist) {
		case 1: // random weight distribution distribution
			points = generateRandomWeightedPoints(n, boundary, weightRange,
					isInteger, isDistinct);
			break;
		case 2: // zipf weight distribution
			points = generateZipfWeightedPoints(n, boundary, weightRange,
					isInteger, isDistinct);
			break;
		case 3: // charminar two-dimensional dataset
			points = generateCharminarWeightedPoints(n, boundary, 100, 100,
					isInteger);
			break;
		case 4: // non-distinct two-dimensional dataset
			// points = generateNonDistinctDataset(n, boundary, 100, 100, true);
			break;
		}
		writeWeightedPointsToFile(points, filePath);
	}

	/**
	 * The output is a list of pairs <value/frequency>. Either value or
	 * frequency or both can follow some distribution
	 * 
	 * @param n
	 * @param min
	 * @param max
	 * @param dist
	 * @param isInteger
	 * @param isRandom
	 * @param valueDistribution
	 * @valueDist = 1 --> uniform...
	 */
	public void generateOneDimDataset2(int n, double min, double max, int dist,
			boolean isInteger, boolean isRandom, int valueDistribution) {
		List<ValueFreq<Double>> valueFreqs = new Vector<ValueFreq<Double>>();
		switch (dist) {
		case 1: // Read a list of values and generate a list of
				// <value/frequency> pairs
			valueFreqs = generateOneDimFromValues("./res/dataset/oned/one_d_mcdonald_1000_1.txt");
			break;
		case 2: // frequency follows zipf distribution, value follows a
				// distribution
			valueFreqs = generateOneDimZipfFreq2(n, min, max, isInteger,
					isRandom, valueDistribution);
			break;
		case 3: // create one-dimensional dataset from two dimensional dataset
				// (apply a rule)
			valueFreqs = generateOneDimFromPoints2(
					"./res/dataset/twod/mcdonald.txt", isInteger);
			break;
		}
		writeValuesToFile(valueFreqs, filePath);
	}

	/**
	 * Generate one-dimensional dataset from two-dimensional dataset from a file
	 * 
	 * @param twoDimInputFile
	 * @param isInteger
	 * @return
	 */
	private Vector<Double> generateOneDimFromPoints(String twoDimInputFile,
			boolean isInteger) {
		// TODO Auto-generated method stub
		Vector<Double> values = new Vector<Double>();
		DataProvider dp = new DataProvider(twoDimInputFile,
				DataTypeEnum.NORMAL_POINT);
		Iterator it = dp.points.iterator();
		while (it.hasNext()) {
			Point point = (Point) it.next();
			double val = point.getY() * 100;
			if (isInteger)
				val = Math.ceil(val);
			values.add(val);
		}
		return values;
	}

	private List<ValueFreq<Double>> generateOneDimFromPoints2(
			String twoDimInputFile, boolean isInteger) {
		// TODO Auto-generated method stub
		List<Double> values = new Vector<Double>();
		DataProvider dp = new DataProvider(twoDimInputFile,
				DataTypeEnum.NORMAL_POINT);
		Iterator it = dp.points.iterator();
		while (it.hasNext()) {
			Point point = (Point) it.next();
			double val = point.getY() * 100;
			if (isInteger)
				val = Math.ceil(val);
			values.add(val);
		}

		Collections.sort(values);
		Stats<Double> stat = new Stats<Double>();
		return stat.getValueFreqs((Double[]) values.toArray(new Double[values
				.size()]));
	}

	/**
	 * Get sorted list of value/freq from a list of values
	 * 
	 * @param twoDimInputFile
	 * @return
	 */
	private List<ValueFreq<Double>> generateOneDimFromValues(
			String twoDimInputFile) {
		DataProvider dp = new DataProvider(twoDimInputFile,
				DataTypeEnum.VALUE_LIST); // read a list
		// of values
		List<Double> values = dp.values;
		Collections.sort(values);
		Stats stat = new Stats();
		return stat.getValueFreqs2(values);
	}

	/**
	 * Write a list of points to a file
	 * 
	 * @param points
	 */
	private void writePointsToFile(Vector<Point> points, String outFilePath) {
		// Create file
		try {
			FileWriter fstream = new FileWriter(outFilePath);
			BufferedWriter out = new BufferedWriter(fstream);
			StringBuffer sb = new StringBuffer();
			Iterator<Point> it = points.iterator();
			while (it.hasNext()) {
				Point point = (Point) it.next();
				sb.append(point.getX() + delimiter.toString() + point.getY());

				if (it.hasNext())
					sb.append("\n");
			}
			out.write(sb.toString());
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		System.out.println("Dataset created!");
	}

	/**
	 * Write a list of points to a file with a primary key
	 * 
	 * @param points
	 */
	private void writePointsToFileWithKey(Vector<Point> points,
			String outFilePath) {
		// Create file
		try {
			FileWriter fstream = new FileWriter(outFilePath);
			BufferedWriter out = new BufferedWriter(fstream);
			StringBuffer sb = new StringBuffer();
			int i = 1;

			Iterator<Point> it = points.iterator();
			while (it.hasNext()) {
				Point point = (Point) it.next();
				sb.append(i++ + delimiter.toString() + +point.getX()
						+ delimiter.toString() + +point.getY());

				if (it.hasNext())
					sb.append('\n');
			}
			out.write(sb.toString());
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		System.out.println("Dataset created!");
	}

	/**
	 * Write a list of weighted points to a file
	 * 
	 * @param points
	 */
	private void writeWeightedPointsToFile(Vector<WeightedPoint> points,
			String outFilePath) {
		// Create file
		try {
			FileWriter fstream = new FileWriter(outFilePath);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("50\t50");

			Iterator<WeightedPoint> it = points.iterator();
			while (it.hasNext()) {
				WeightedPoint point = (WeightedPoint) it.next();
				out.write("\n");
				out.write(point.getX() + "\t" + point.getY() + "\t"
						+ point.getWeight());
			}
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		System.out.println("Dataset created!");

	}

	/**
	 * Write a list of integer to a file
	 * 
	 * @param points
	 */
	private void writeIntegersToFile(Vector<Double> values, String outFilePath) {
		// Create file
		try {
			FileWriter fstream = new FileWriter(outFilePath);
			BufferedWriter out = new BufferedWriter(fstream);
			Iterator<Double> it = values.iterator();
			if (it.hasNext())
				out.write(((Double) it.next()).toString());
			while (it.hasNext()) {
				Double value = (Double) it.next();
				out.write("\n");
				out.write(value.toString());
			}
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		System.out.println("Dataset created!");
	}

	private void writeValuesToFile(List<ValueFreq<Double>> valueFreqs,
			String outFilePath) {
		// Create file
		try {
			FileWriter fstream = new FileWriter(outFilePath);
			BufferedWriter out = new BufferedWriter(fstream);
			Iterator<ValueFreq<Double>> it = valueFreqs.iterator();
			if (it.hasNext()) {
				ValueFreq<Double> vf = (ValueFreq<Double>) it.next();
				out.write(vf.getValue() + "\t" + vf.getFreq());
			}
			while (it.hasNext()) {
				ValueFreq<Double> vf = it.next();
				out.write("\n");
				out.write(vf.getValue() + "\t" + vf.getFreq());
			}
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		System.out.println("Dataset created!");
	}

	/**
	 * Write a list of integer to a file with a primary key
	 * 
	 * @param values
	 * @param string
	 */
	private void writeIntegersToFileWithKey(Vector<Double> values,
			String outFilePath) {
		// TODO Auto-generated method stub
		try {
			FileWriter fstream = new FileWriter(outFilePath);
			BufferedWriter out = new BufferedWriter(fstream);
			Iterator<Double> it = values.iterator();
			int i = 1;
			if (it.hasNext())
				out.write(i++ + "\t" + ((Double) it.next()).toString());
			while (it.hasNext()) {
				Double value = (Double) it.next();
				out.write("\n");
				out.write(i++ + "\t" + value.toString());
			}
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		System.out.println("Dataset created!");
	}

}
