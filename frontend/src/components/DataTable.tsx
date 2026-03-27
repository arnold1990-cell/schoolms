export function DataTable({ rows, maxCols = 6 }: { rows: Record<string, unknown>[]; maxCols?: number }) {
  const columns = rows[0] ? Object.keys(rows[0]).slice(0, maxCols) : [];

  return (
    <table className="table">
      <thead>
        <tr>
          {columns.map((column) => (
            <th key={column}>{column}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, rowIndex) => (
          <tr key={rowIndex}>
            {columns.map((column) => {
              const value = row[column];
              const cell = typeof value === 'object' && value !== null ? JSON.stringify(value) : String(value ?? '-');
              return <td key={`${rowIndex}-${column}`}>{cell}</td>;
            })}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
