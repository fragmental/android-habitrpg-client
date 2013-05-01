package de.uvwxy.habitrpg;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import de.uvwxy.habitrpg.R;
import de.uvwxy.habitrpg.api.HabitConnectionV1;
import de.uvwxy.habitrpg.api.HabitConnectionV1.ServerResultCallback;
import de.uvwxy.habitrpg.sprites.HabitColors;

public class ExpandableTaskViewAdapter extends BaseExpandableListAdapter {
	private Context ctx;
	private ArrayList<ExpandableTask> listOfAllTasks = null;
	private LayoutInflater inf;
	private HabitConnectionV1 habitCon = null;

	private static final boolean BUY = true;
	private static final boolean UP = true;
	private static final boolean DOWN = false;
	private static final boolean COMPLETED = true;
	private static final boolean UNCOMPLETED = false;
	// this is never used
	private static final boolean ISCHECKBOX = true;

	private ServerResultCallback serverResultCallback = null;

	private void habitClick(View v, final String taskId, final boolean upOrCompleted) {

		if (v instanceof Button) {
			v.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					final ProgressDialog waitingDialog = ProgressDialog.show(ctx, "Communicating", "Please wait...", true);
					waitingDialog.setProgress(10);
					waitingDialog.show();
					
					Thread t = new Thread(new Runnable() {

						@Override
						public void run() {
							try {
								
								String result = habitCon.updateTask(taskId, upOrCompleted);
								serverResultCallback.serverReply(result);
								waitingDialog.dismiss();
							} catch (ClientProtocolException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
					t.start();
				}
			});
		}

		if (v instanceof CheckBox) {
			CheckBox cb = (CheckBox) v;
			cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

					boolean direction = false;
					if (isChecked) {
						direction = COMPLETED;
					} else {
						direction = UNCOMPLETED;
					}
					final boolean fDirection = direction;

					Thread t = new Thread(new Runnable() {

						@Override
						public void run() {
							try {
								String result = habitCon.updateTask(taskId, fDirection);
								serverResultCallback.serverReply(result);
							} catch (ClientProtocolException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
					t.start();
				}
			});
		}

	}

	public ExpandableTaskViewAdapter(Context ctx, ArrayList<ExpandableTask> list, HabitConnectionV1 habitCon, ServerResultCallback src) {
		if (ctx == null || list == null || habitCon == null || src == null) {
			throw new RuntimeException("Context or list or habitCon was null. uh oh..");
		}
		this.ctx = ctx;
		this.listOfAllTasks = list;
		this.habitCon = habitCon;
		this.serverResultCallback = src;
		this.inf = LayoutInflater.from(ctx);

	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		try {
			return listOfAllTasks.get(groupPosition).getList().getJSONObject(childPosition);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

		Object o = listOfAllTasks.get(groupPosition);
		if (!(o instanceof ExpandableTask)) {
			return null;
		}

		ExpandableTask e = ((ExpandableTask) o);

		switch (e.getType()) {
		case DAILY:
			return getDailyView(groupPosition, childPosition, isLastChild, convertView, parent);

		case HABIT:
			return getHabitView(groupPosition, childPosition, isLastChild, convertView, parent);

		case REWARD:
			return getRewardView(groupPosition, childPosition, isLastChild, convertView, parent);

		case TODO:
			return getTodoView(groupPosition, childPosition, isLastChild, convertView, parent);

		default:
			return null;

		}

	}

	private View getHabitView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

		convertView = inf.inflate(R.layout.expandable_habit, parent, false);

		final RelativeLayout rlHabit = (RelativeLayout) convertView.findViewById(R.id.rlHabit);
		final TextView tvHabit = (TextView) convertView.findViewById(R.id.tvHabit);
		final Button btnPlus = (Button) convertView.findViewById(R.id.btnPlus);
		final Button btnMinus = (Button) convertView.findViewById(R.id.btnMinus);

		ExpandableTask e = listOfAllTasks.get(groupPosition);

		if (e != null) {
			JSONArray list = e.getList();

			try {
				JSONObject h = list.getJSONObject(childPosition);

				if (!h.getBoolean("up")) {
					btnPlus.setVisibility(View.INVISIBLE);
				} else {
					habitClick(btnPlus, h.getString("id"), UP);
				}

				if (!h.getBoolean("down")) {
					btnMinus.setVisibility(View.INVISIBLE);
				} else {
					habitClick(btnMinus, h.getString("id"), DOWN);
				}

				tvHabit.setText(h.getString("text"));

				rlHabit.setBackgroundColor(HabitColors.colorFromValue(h.getDouble("value")));
			} catch (JSONException e1) {
				tvHabit.setText("[Error]}\n" + e1.getMessage());
				tvHabit.setOnClickListener(GUIHelpers.mkToastListener(ctx, e1.getMessage()));
				e1.printStackTrace();
			}
		}

		return convertView;
	}

	private View getDailyView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

		convertView = inf.inflate(R.layout.expandable_daily, parent, false);

		final RelativeLayout rlDaily = (RelativeLayout) convertView.findViewById(R.id.rlDaily);
		final CheckBox cbDaily = (CheckBox) convertView.findViewById(R.id.cbDaily);
		final ExpandableTask e = listOfAllTasks.get(groupPosition);

		if (e != null && cbDaily != null) {
			JSONArray list = e.getList();

			try {
				JSONObject h = list.getJSONObject(childPosition);

				cbDaily.setChecked(h.getBoolean("completed"));

				//				if (cbDaily.isChecked()){
				//					// we can not uncheck things with the api?
				//					cbDaily.setEnabled(false);
				//				}

				cbDaily.setText(h.getString("text"));
				habitClick(cbDaily, h.getString("id"), ISCHECKBOX);
				rlDaily.setBackgroundColor(HabitColors.colorFromValue(h.getDouble("value")));
			} catch (JSONException e1) {
				cbDaily.setText("[Error]}\n" + e1.getMessage());
				cbDaily.setOnClickListener(GUIHelpers.mkToastListener(ctx, e1.getMessage()));
				e1.printStackTrace();
			}
		}

		return convertView;
	}

	private View getTodoView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

		convertView = inf.inflate(R.layout.expandable_todo, parent, false);

		final RelativeLayout rlTodo = (RelativeLayout) convertView.findViewById(R.id.rlTodo);
		final CheckBox cbTodo = (CheckBox) convertView.findViewById(R.id.cbTodo);

		ExpandableTask e = listOfAllTasks.get(groupPosition);

		if (e != null && cbTodo != null) {
			JSONArray list = e.getList();

			try {
				JSONObject h = list.getJSONObject(childPosition);

				cbTodo.setChecked(h.getBoolean("completed"));

				cbTodo.setText(h.getString("text"));
				habitClick(cbTodo, h.getString("id"), ISCHECKBOX);
				rlTodo.setBackgroundColor(HabitColors.colorFromValue(h.getDouble("value")));

			} catch (JSONException e1) {
				cbTodo.setText("[Error]}\n" + e1.getMessage());
				cbTodo.setOnClickListener(GUIHelpers.mkToastListener(ctx, e1.getMessage()));
				e1.printStackTrace();
			}
		}

		return convertView;
	}

	private View getRewardView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

		convertView = inf.inflate(R.layout.expandable_reward, parent, false);

		final Button btnRewardBuy = (Button) convertView.findViewById(R.id.btnRewardBuy);
		final ImageView ivRewardIcon = (ImageView) convertView.findViewById(R.id.ivRewardIcon);
		final TextView tvRewardDescription = (TextView) convertView.findViewById(R.id.tvRewardDescription);
		final TextView tvRewardPrice = (TextView) convertView.findViewById(R.id.tvRewardPrice);

		ExpandableTask e = listOfAllTasks.get(groupPosition);

		if (e != null && tvRewardPrice != null) {
			JSONArray list = e.getList();

			try {
				JSONObject h = list.getJSONObject(childPosition);

				tvRewardPrice.setText("" + h.getInt("value") + "G");
				// TODO: COIN: btnRewardBuy.setBackgroundResource(R.drawable.)

				tvRewardDescription.setText(h.getString("text"));
				habitClick(btnRewardBuy, h.getString("id"), BUY);

			} catch (JSONException e1) {
				tvRewardDescription.setText("[Error]}\n" + e1.getMessage());
				tvRewardDescription.setOnClickListener(GUIHelpers.mkToastListener(ctx, e1.getMessage()));
				e1.printStackTrace();
			}
		}

		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		if (listOfAllTasks.get(groupPosition).getList() != null) {
			return listOfAllTasks.get(groupPosition).getList().length();
		} else {
			return 0;
		}
	}

	@Override
	public Object getGroup(int groupPosition) {
		return listOfAllTasks.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return listOfAllTasks.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		convertView = inf.inflate(R.layout.expandable_group, parent, false);

		TextView tvGroupTitle = (TextView) convertView.findViewById(R.id.tvGroupTitle);

		tvGroupTitle.setText(listOfAllTasks.get(groupPosition).getTitle());

		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		// TODO: proper implementation for this?
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

}