package fun.keyle.MinIO.entry;

import lombok.Data;

/**
 * 文件基本信息
 */
@Data
public class Fileinfo {
	String filename;
	Boolean directory;
}