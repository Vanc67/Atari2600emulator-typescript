class_name SaveFileStore
extends RefCounted



























const TEMP_SUFFIX: = ".tmp"
const BACKUP_SUFFIX: = ".bak"


var last_error_message: = ""



func write(path: String, text: String) -> Error:
	last_error_message = ""
	if text.is_empty():
		return _fail(ERR_INVALID_DATA, "refusing to write empty save data")
	var directory_error: = DirAccess.make_dir_recursive_absolute(path.get_base_dir())
	if directory_error != OK:
		return _fail(directory_error, "cannot create save folder '%s'" % path.get_base_dir())

	var temp_path: = path + TEMP_SUFFIX
	var file: = FileAccess.open(temp_path, FileAccess.WRITE)
	if file == null:
		return _fail(FileAccess.get_open_error(), "cannot create temporary file '%s'" % temp_path)
	var stored: = file.store_string(text)
	file.flush()
	var write_error: = file.get_error()
	file.close()
	if not stored or write_error != OK:
		DirAccess.remove_absolute(temp_path)
		return _fail(write_error if write_error != OK else ERR_FILE_CANT_WRITE, 
			"writing the temporary file failed (disk full or not writable?)")
	if FileAccess.get_file_as_string(temp_path) != text:
		DirAccess.remove_absolute(temp_path)
		return _fail(ERR_FILE_CORRUPT, "the temporary file did not read back correctly")

	if FileAccess.file_exists(path):
		var backup_path: = path + BACKUP_SUFFIX
		var copy_error: = DirAccess.copy_absolute(path, backup_path)
		if copy_error != OK or _file_size(backup_path) != _file_size(path):
			DirAccess.remove_absolute(temp_path)
			return _fail(copy_error if copy_error != OK else ERR_FILE_CORRUPT, 
				"could not back up the existing save; it was left untouched")

	var rename_error: = DirAccess.rename_absolute(temp_path, path)
	if rename_error != OK and FileAccess.file_exists(path) and FileAccess.file_exists(path + BACKUP_SUFFIX):


		if DirAccess.remove_absolute(path) == OK:
			rename_error = DirAccess.rename_absolute(temp_path, path)
			if rename_error != OK:
				DirAccess.copy_absolute(path + BACKUP_SUFFIX, path)
	if rename_error != OK:
		return _fail(rename_error, "could not replace the save file; the previous save was kept"
			+ (" and the new data remains in '%s'" % temp_path if FileAccess.file_exists(temp_path) else ""))
	return OK






func read(path: String, is_valid: Callable) -> Dictionary:
	var notes: = PackedStringArray()
	for candidate: Array in [[path, "primary"], [path + TEMP_SUFFIX, "pending"], [path + BACKUP_SUFFIX, "backup"]]:
		var candidate_path: String = candidate[0]
		if not FileAccess.file_exists(candidate_path):
			continue
		var text: = FileAccess.get_file_as_string(candidate_path)
		if is_valid.call(text):
			if candidate[1] == "pending":



				DirAccess.rename_absolute(candidate_path, path)
				notes.append("recovered an interrupted save")
			elif candidate[1] == "backup":
				notes.append("the save file was missing or damaged; loaded the previous backup")
			return {"text": text, "source": candidate[1], "notes": notes}
		notes.append("ignored unusable file '%s'" % candidate_path.get_file())
	return {"text": "", "source": "", "notes": notes}


func exists(path: String) -> bool:
	return FileAccess.file_exists(path) or FileAccess.file_exists(path + TEMP_SUFFIX)\
	or FileAccess.file_exists(path + BACKUP_SUFFIX)


static func _file_size(path: String) -> int:
	var file: = FileAccess.open(path, FileAccess.READ)
	return file.get_length() if file else -1


func _fail(error: Error, message: String) -> Error:
	last_error_message = "%s (%s)" % [message, error_string(error)]
	return error
