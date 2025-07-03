def get_unique_lines(file1_path, file2_path, output_path):
    with open(file1_path, 'r', encoding='utf-8') as f1:
        lines1 = set(line.strip() for line in f1 if line.strip())

    with open(file2_path, 'r', encoding='utf-8') as f2:
        lines2 = set(line.strip() for line in f2 if line.strip())

    # Symmetric difference: lines that are in either file1 or file2, but not both
    unique_lines = lines1.symmetric_difference(lines2)

    with open(output_path, 'w', encoding='utf-8') as out:
        for line in sorted(unique_lines):
            out.write(line + '\n')

    print(f"Unique lines written to {output_path}")


# Example usage:
if __name__ == "__main__":
    file1 = "registry_dump_1.21.5.txt"
    file2 = "registry_dump_1.21.txt"
    output = "unique_lines.txt"
    get_unique_lines(file1, file2, output)
