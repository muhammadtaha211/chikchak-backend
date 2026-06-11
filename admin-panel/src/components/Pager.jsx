import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Typography from '@mui/material/Typography';

// The backend returns plain lists without a total count, so this is a simple
// previous/next pager: Next is disabled when the current page came back short.
export default function Pager({ page, size, count, onPage, onSize }) {
  return (
    <Stack direction="row" spacing={2} alignItems="center" justifyContent="flex-end" sx={{ p: 1.5 }}>
      <TextField
        select
        size="small"
        label="Rows"
        value={size}
        onChange={(e) => onSize(Number(e.target.value))}
        sx={{ width: 90 }}
      >
        {[10, 20, 50].map((n) => (
          <MenuItem key={n} value={n}>
            {n}
          </MenuItem>
        ))}
      </TextField>
      <Typography variant="body2">Page {page + 1}</Typography>
      <Button size="small" disabled={page === 0} onClick={() => onPage(page - 1)}>
        Previous
      </Button>
      <Button size="small" disabled={count < size} onClick={() => onPage(page + 1)}>
        Next
      </Button>
    </Stack>
  );
}
