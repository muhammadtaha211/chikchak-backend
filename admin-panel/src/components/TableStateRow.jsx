import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import CircularProgress from '@mui/material/CircularProgress';
import Alert from '@mui/material/Alert';

// Single full-width body row shown while a table is loading, errored, or empty.
export default function TableStateRow({ colSpan, loading, error, emptyMessage = 'Nothing to show' }) {
  return (
    <TableRow>
      <TableCell colSpan={colSpan} align="center" sx={{ py: 4, color: 'text.secondary', border: 0 }}>
        {loading ? (
          <CircularProgress size={28} />
        ) : error ? (
          <Alert severity="error">{error}</Alert>
        ) : (
          emptyMessage
        )}
      </TableCell>
    </TableRow>
  );
}
