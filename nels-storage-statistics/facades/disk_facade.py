from os import path,listdir

def get_tree_size(folder):
    """Return total size of files in given path and subdirs."""
    total = 0
    for child in listdir(folder):
        child_pth = path.join(folder,child)
        if path.islink(child_pth):
            continue
        if path.isdir(child_pth):
            total += get_tree_size(child_pth)
        else:
            total += path.getsize(child_pth)
    return total


def get_folder_size(folder):
    if not path.exists(folder) or not path.isdir(folder):
        raise("Non existing folder")
    return get_tree_size(folder)