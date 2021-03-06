package ws.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class Version {

	private static final boolean[] PRINT_FLAG_OFF = new boolean[] { true };

	public static void main(String[] args) throws Exception {
		getVersion();
	}

	public static boolean getVersion() {
		// E:\\Zhao\\Tools\\ChromeStandaloneSetup.exe
		// E:\\DG2013_Final_1035E.exe
		// E:\\Zhao\\Developments\\vc\\MSDev98\\Bin\\MSDEV.EXE
		// E:\\Zhao\\Tools\\office2010Standard\\setup.exe
		File file = new File("E:\\Zhao\\Tools\\EditPlus\\epp211a1052.exe");
		RandomAccessFile raf = null;

		List<Byte> buffer = null;

		Long length = 0l;
		String val = null;

		try {
			raf = new RandomAccessFile(file, "r");
			System.out.println("文件字节数：" + raf.length());

			boolean flg = true;
			byte[] bs = new byte[1];
			raf.seek(736);
			for (int i = 0; i < raf.length(); i++) {
				raf.read(bs);
				if (0 == (int) bs[0] && flg) {
					System.out.println("字节(" + raf.getFilePointer() + ") 的值为 0 .");
					flg = false;
				} else if (0 == (int) bs[0]) {
					System.out.println("字节(" + raf.getFilePointer() + ") 的值为 0 .");
					break;
				}
			}
			raf.seek(0);
			buffer = readBytes(raf, 64);
			val = getCharsbyB(buffer.subList(0, 2));
			if (!"MZ".equals(val)) {
				return false;
			}

			length = getLengthofDbyB(buffer.subList(60, 63));
			raf.seek(length);
			buffer = readBytes(raf, 24);
			val = getCharsbyB(buffer.subList(0, 2));
			if (!"PE".equals(val)) {
				return false;
			}
			// machine
			String machine = getHexStrbyB(buffer.subList(4, 6));
			// NumberOfSections
			long numOfSec = getLengthofDbyB(buffer.subList(6, 8));
			// SizeOfOptionalHeader
			length = getLengthofDbyB(buffer.subList(20, 22));
			buffer = readBytes(raf, length.intValue() - 128);
			// 镜像基址:ImageBase
			System.out.print("镜像基址:");
			long baseAddr = getLengthofDbyB(buffer.subList(28, 32));
			// SectionAlignment 内存对齐
			System.out.print("内存对齐:");
			length = getLengthofDbyB(buffer.subList(32, 36));
			// FileAlignment 文件对齐
			System.out.print("文件对齐:");
			length = getLengthofDbyB(buffer.subList(36, 40));
			System.out.print("头大小:");
			// SizeOfHeaders 头大小 PE文件第一节的文件偏移量。
			length = getLengthofDbyB(buffer.subList(60, 64));

			// 数据目录
			buffer = readBytes(raf, 128);
			// Resource Table(.rsrc)-VirtualAddress(数据的RVA)
			System.out.print("ResourceTable-数据RVA:");
			length = getLengthofDbyB(buffer.subList(16, 20));
			// Resource Table(.rsrc)-Size(数据的大小)
			System.out.print("ResourceTable-数据大小:");
			length = getLengthofDbyB(buffer.subList(20, 24));
			long secHeadPointer = raf.getFilePointer();

			long virtualSize = 0l;
			long virtualAddress = 0l;
			long sizeOfRawData = 0l;
			long pointerToRawData = 0l;
			boolean srcFlg = false;
			for (int i = 0; i < numOfSec; i++) {
				// 节头
				buffer = readBytes(raf, 40);
				// Name
				val = getCharsbyB(buffer.subList(0, 8));
				virtualSize = getLengthofDbyB(buffer.subList(8, 12));
				virtualAddress = getLengthofDbyB(buffer.subList(12, 16));
				sizeOfRawData = getLengthofDbyB(buffer.subList(16, 20));
				pointerToRawData = getLengthofDbyB(buffer.subList(20, 24));
				if (".rsrc   ".equals(val)) {
					srcFlg = true;
					System.out.println("文件偏移量:" + raf.getFilePointer());
					break;
				}
			}

			if (!srcFlg) {
				return false;
			}

			String characteristics = getBinaryStrbyB(buffer.subList(36, 40));

			srcFlg = false;
			raf.seek(/* pointerToRawData *//* virtualAddress + baseAddr */pointerToRawData);
			System.out.println("文件偏移量:" + raf.getFilePointer());
			buffer = readBytes(raf, 16);
			length = getLengthofDbyB(buffer.subList(12, 14)) + getLengthofDbyB(buffer.subList(14, 16));
			long type = 0l;
			for (int i = 0; i < length; i++) {
				buffer = readBytes(raf, 8);
				String nameFlg = getBinaryStrbyB(buffer.subList(3, 4), PRINT_FLAG_OFF);
				type = getLengthofDbyB(buffer.subList(0, 4), PRINT_FLAG_OFF);
				if ("0".equals(nameFlg.substring(0, 1)) && 16 == type) {

					String pointFlg = getBinaryStrbyB(buffer.subList(7, 8));
					if ("1".equals(pointFlg.substring(0, 1))) {
						String sSubAddr = getBinaryStrbyB(buffer.subList(4, 8));
						int iSubAddr = Integer.parseInt(sSubAddr.substring(1), 2);
						raf.seek(pointerToRawData + iSubAddr);
					}
					srcFlg = true;
					break;
				}
			}

			if (!srcFlg) {
				return false;
			}

			long srcRVA = 0l;
			long srcSize = 0l;
			if (loopSrcTable(raf, pointerToRawData)) {
				buffer = readBytes(raf, 16);
				srcRVA = getLengthofDbyB(buffer.subList(0, 4));
				srcSize = getLengthofDbyB(buffer.subList(4, 8));

				raf.seek(srcRVA - virtualAddress + pointerToRawData);
				buffer = readBytes(raf, (int) srcSize);

				// for (int i = 0; i < (srcSize / 2); i++) {
				// char version = (char) (buffer.get(i) & 0xff);
				// }
				StringBuilder sVer = new StringBuilder();
				long version = getLengthofDbyB(buffer.subList(48, 50));
				sVer.append(".").append(version);
				version = getLengthofDbyB(buffer.subList(50, 52));
				sVer.insert(0, version);
				version = getLengthofDbyB(buffer.subList(54, 56));
				sVer.append(".").append(version);
				version = getLengthofDbyB(buffer.subList(52, 54));
				sVer.append(".").append(version);
				System.out.println("version : " + sVer);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			if (null != raf) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return false;
	}

	/**
	 * 找到IMAGE_RESOURCE_DATA_ENTRY的位置，并将读取指针指向该起始位置。
	 * 
	 * @param raf
	 * @param srcBaseAddr
	 * @return
	 * @throws Exception
	 */
	private static Boolean loopSrcTable(RandomAccessFile raf, long srcBaseAddr) throws Exception {

		List<Byte> buffer = null;
		long length = 0l;

		buffer = readBytes(raf, 16);
		length = getLengthofDbyB(buffer.subList(12, 14)) + getLengthofDbyB(buffer.subList(14, 16));
		if (length != 1) {
			return false;
		}
		buffer = readBytes(raf, 8);
		String nameFlg = getBinaryStrbyB(buffer.subList(3, 4));
		if ("0".equals(nameFlg.substring(0, 1))) {
			System.out.println("Name is ID");
		}

		String pointFlg = getBinaryStrbyB(buffer.subList(7, 8));
		if ("1".equals(pointFlg.substring(0, 1))) {

			String sSubAddr = getBinaryStrbyB(buffer.subList(4, 8));
			int iSubAddr = Integer.parseInt(sSubAddr.substring(1), 2);
			raf.seek(srcBaseAddr + iSubAddr);
			return loopSrcTable(raf, srcBaseAddr);

		} else if ("0".equals(pointFlg.substring(0, 1))) {

			String sSubAddr = getBinaryStrbyB(buffer.subList(4, 8));
			int iSubAddr = Integer.parseInt(sSubAddr.substring(1), 2);
			raf.seek(srcBaseAddr + iSubAddr);
			return true;
		}

		return false;
	}

	private static List<Byte> readBytes(RandomAccessFile raf, int length) {
		List<Byte> lstB = new ArrayList<>();

		byte[] buffer = new byte[length];
		try {
			raf.read(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (byte item : buffer) {
			lstB.add(item);
		}
		return lstB;
	}

	private static long getDbyB(List<Byte> bytes) {

		int num = 0;
		for (int i = 0; i < bytes.size(); i++) {
			num = 256 * num + (bytes.get(bytes.size() - 1 - i) & 0xff);
		}
		return num;

		// long num = 0;
		//
		// long mupliy = 1;
		//
		// for (int i = 0, size = bytes.size(); i < size; i++) {
		// for (int j = 0; j < i; j++) {
		// mupliy = mupliy * 256;
		// }
		// num = num + (bytes.get(i) & 0xff) * mupliy;
		// }
		//
		// return num;
	}

	private static long getLengthofDbyB(List<Byte> bytes, boolean... flg) {
		long length = getDbyB(bytes);
		if (null == flg || flg.length == 0) {
			System.out.println("多字节值总长：" + length); // TODO
		}
		return length;
	}

	private static String getHexStrbyB(List<Byte> bytes) {
		String hexStr = Long.toHexString(getDbyB(bytes));
		System.out.println("多字节值16进制：0x" + hexStr); // TODO
		return hexStr;
	}

	private static String getBinaryStrbyB(List<Byte> bytes, boolean... flg) {
		String sBinaryStr = null;
		StringBuilder sBiLiterals = new StringBuilder();
		for (Byte item : bytes) {
			sBinaryStr = Long.toBinaryString((long) (item & 0xff));
			sBinaryStr = String.format("%08d", 0).concat(sBinaryStr);
			sBinaryStr = sBinaryStr.substring(sBinaryStr.length() - 8);
			sBiLiterals.insert(0, sBinaryStr);
		}

		// String BinaryStr = Long.toBinaryString(getDbyB(bytes));
		// sBinaryStr = String.format("%08d", 0).concat(sBinaryStr);
		// sBinaryStr = sBinaryStr.substring(sBinaryStr.length() - 8);
		if (null == flg || flg.length == 0) {
			System.out.println("多字节值2进制：B" + sBiLiterals); // TODO
		}
		return sBiLiterals.toString();
	}

	private static String getCharsbyB(List<Byte> bytes) {

		StringBuilder str = new StringBuilder("");

		for (Byte item : bytes) {
			str.append((char) item.byteValue());
		}

		System.out.println("多字节ASCII值：" + str); // TODO
		return str.toString();
	}
}
