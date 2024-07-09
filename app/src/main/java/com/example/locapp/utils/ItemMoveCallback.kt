import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.locapp.ui.dashboard.accepted_orders.HomeAdapter
import com.example.locapp.ui.home.HomeViewModel

class ItemMoveCallback(private val adapter: HomeAdapter, private val viewModel: HomeViewModel) : ItemTouchHelper.Callback() {

    private var dragStartPosition: Int? = null
    private var dragEndPosition: Int? = null

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // NOTHING for now
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        dragStartPosition = viewHolder.adapterPosition
        dragEndPosition = target.adapterPosition
        return false
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && dragStartPosition != null && dragEndPosition != null) {
            adapter.onItemMove(dragStartPosition!!, dragEndPosition!!)
            viewModel.updateOrderIndex(adapter.currentList[dragStartPosition!!].orderId, dragEndPosition!!)

            dragStartPosition = null
            dragEndPosition = null
        }
    }
}
