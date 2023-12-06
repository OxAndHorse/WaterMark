package cn.ac.iie.pkcgroup.dws.core.db.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

/**
 * 定义水印类
 */
@Data
public class Watermark {
	private int length;
	ArrayList<Integer> binary;

	/**
	 * @param length the length of watermark
	 * @param binary ArrayList<Integer> type watermark
	 */
	public Watermark(int length, ArrayList<Integer> binary){
		this.length = length;
		this.binary = binary;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		for (Integer bit:
			 binary) {
			stringBuilder.append(bit.toString());
		}
		return stringBuilder.toString();
	}


	public String printAsBitString() {
		StringBuilder stringBuilder = new StringBuilder();
		for (int bit:
				binary) {
			if (bit == 0) stringBuilder.append("0");
			else stringBuilder.append("1");
		}
		return stringBuilder.toString();
	}
}
